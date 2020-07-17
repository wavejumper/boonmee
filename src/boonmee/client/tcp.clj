(ns boonmee.client.tcp
  (:require [clojure.core.async :as async]
            [boonmee.client.stdio]
            [integrant.core :as ig]
            [net.tcp.server :as tcp]
            [boonmee.logging :as log])
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
                                         :tsserver-req-ch  (ig/ref :chan/tsserver-req-ch)}
   :boonmee/server                      {:tsserver-resp-ch (ig/ref :chan/tsserver-resp-ch)
                                         :tsserver-req-ch  (ig/ref :chan/tsserver-req-ch)
                                         :client-req-ch    (ig/ref :chan/client-req-ch)
                                         :client-resp-ch   (ig/ref :chan/client-resp-ch)
                                         :ctx              {}}
   :boonmee/stdio-client                {:client-req-ch  (ig/ref :chan/client-req-ch)
                                         :client-resp-ch (ig/ref :chan/client-resp-ch)
                                         :in             (:in opts)
                                         :out            (:out opts)}
   :logger/tcp-logger                   {:client-id (:client-id opts)}})

(defn config
  [opts]
  {:tcp/socket {:port      (:port opts)
                :heartbeat (:heartbeat opts)}})

(defn heartbeat-loop
  [heartbeat-ms p {:keys [ts]}]
  (async/go-loop []
    (let [now           (System/currentTimeMillis)
          last-seen-msg @ts]
      (if (< (- now last-seen-msg) heartbeat-ms)
        (do (async/<! (async/timeout 1000))
            (recur))
        (deliver p true)))))

(defn handle-connection
  [conns {:keys [heartbeat] :as opts} ^Socket socket]
  (try
    (let [id             (str (UUID/randomUUID))
          in             (InputStreamReader. (.getInputStream socket))
          out            (PrintWriter. (.getOutputStream socket) true)
          opts           (assoc opts :client-id id :in in :out out)
          sys            (ig/init (handler-config opts))
          close-promise  (promise)
          heartbeat-loop (heartbeat-loop heartbeat close-promise (:boonmee/stdio-client sys))
          conn           {:in             in
                          :out            out
                          :sys            sys
                          :close-promise  close-promise
                          :heartbeat-loop heartbeat-loop}]
      (log/info "New connection")
      (swap! conns conj conn)
      (deref close-promise)
      (log/info "Connection closed")
      (async/close! heartbeat-loop)
      (ig/halt! sys)
      (.close socket)
      (.close in)
      (.close out))
    (catch Throwable e
      (.printStackTrace e))))

(defmethod ig/init-key
  :tcp/socket
  [_ {:keys [port] :as opts}]
  (println "boonmee listening on " port)
  (let [conns   (atom [])
        handler (partial handle-connection conns opts)
        server  (tcp/tcp-server :port port :handler handler)]
    (tcp/start server)
    {:server server
     :conns  conns}))

(defmethod ig/halt-key!
  :tcp/socket
  [_ {:keys [server conns]}]
  (doseq [{:keys [close-promise]} @conns]
    (deliver close-promise true))
  (tcp/stop server)
  {})