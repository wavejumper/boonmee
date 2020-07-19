(ns boonmee.client.tcp
  (:require [clojure.core.async :as async]
            [boonmee.client.stdio]
            [integrant.core :as ig]
            [boonmee.logging :as log]
            [boonmee.tcp-server :as tcp])
  (:import (java.net Socket)
           (java.util UUID)
           (java.io InputStreamReader PrintWriter)))

(defn handler-config
  [opts]
  {[:async/chan :chan/tsserver-resp-ch] {}
   [:async/chan :chan/tsserver-req-ch]  {}
   [:async/chan :chan/client-resp-ch]   {}
   [:async/chan :chan/client-req-ch]    {}
   :boonmee/tsserver                    {:tsserver-resp-ch (ig/ref :chan/tsserver-resp-ch)
                                         :tsserver-req-ch  (ig/ref :chan/tsserver-req-ch)
                                         :logger           (ig/ref :logger/stdout-logger)}
   :boonmee/server                      {:tsserver-resp-ch (ig/ref :chan/tsserver-resp-ch)
                                         :tsserver-req-ch  (ig/ref :chan/tsserver-req-ch)
                                         :client-req-ch    (ig/ref :chan/client-req-ch)
                                         :client-resp-ch   (ig/ref :chan/client-resp-ch)
                                         :logger           (ig/ref :logger/stdout-logger)
                                         :ctx              {:client-id    (:client-id opts)
                                                            :heartbeat-ms (:heartbeat opts)
                                                            :env          (:env opts)
                                                            :client       :tcp}}
   :boonmee/stdio-client                {:client-req-ch  (ig/ref :chan/client-req-ch)
                                         :client-resp-ch (ig/ref :chan/client-resp-ch)
                                         :in             (ig/ref :tcp/socket-reader)
                                         :out            (ig/ref :tcp/socket-writer)}
   :logger/stdout-logger                {:ctx {:client-id (:client-id opts)}}
   :tcp/socket-reader                   {:socket (:socket opts)}
   :tcp/socket-writer                   {:socket (:socket opts)}
   :tcp/connection-heartbeat            {:client       (ig/ref :boonmee/stdio-client)
                                         :heartbeat-ms (:heartbeat opts)
                                         :logger       (ig/ref :logger/stdout-logger)
                                         :promise      (:promise opts)}})

(defmethod ig/init-key :tcp/socket-reader
  [_ {:keys [^Socket socket]}]
  (InputStreamReader. (.getInputStream socket)))

(defmethod ig/halt-key! :tcp/socket-reader
  [_ ^InputStreamReader reader]
  (.close reader))

(defmethod ig/init-key :tcp/socket-writer
  [_ {:keys [^Socket socket]}]
  (PrintWriter. (.getOutputStream socket) true))

(defmethod ig/halt-key! :tcp/socket-writer
  [_ ^PrintWriter writer]
  (.close writer))

(defmethod ig/init-key :tcp/connection-heartbeat
  [_ {:keys [client logger heartbeat-ms promise]}]
  (async/go-loop []
    (when-not (realized? promise)
      (let [now           (System/currentTimeMillis)
            last-seen-msg (-> client :ts deref)]
        (if (< (- now last-seen-msg) heartbeat-ms)
          (do (async/<! (async/timeout 1000))
              (recur))
          (do (log/warnf logger "Connection timeout after %s" heartbeat-ms)
              (deliver promise true)))))))

(defmethod ig/halt-key! :tcp/connection-heartbeat
  [_ ch]
  (async/close! ch))

(defn config
  [opts]
  {:tcp/server           {:port      (:port opts)
                          :logger    (ig/ref :logger/stdout-logger)
                          :heartbeat (:heartbeat opts)}
   :logger/stdout-logger {}})

(defn handle-connection
  [logger opts ^Socket socket close-promise]
  (try
    (let [client-id (str (UUID/randomUUID))
          opts      (assoc opts :client-id client-id :promise close-promise :socket socket)
          sys       (ig/init (handler-config opts))]
      (log/infof logger "New connection: %s" client-id)
      (fn []
        (log/infof logger "Connection closed: %s" client-id)
        (ig/halt! sys)))
    (catch Throwable e
      (println e)
      (.printStackTrace e))))

(defmethod ig/init-key
  :tcp/server
  [_ {:keys [port logger] :as opts}]
  (log/infof logger "boonmee listening on %s" port)
  (let [handler (partial handle-connection logger opts)
        server  (tcp/tcp-server :port port :handler handler)]
    (tcp/start server)))

(defmethod ig/halt-key!
  :tcp/server
  [_ server]
  (tcp/stop server))