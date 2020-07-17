(ns boonmee.client.stdio
  (:require [boonmee.server]
            [boonmee.tsserver.server]
            [boonmee.util :as util]
            [clojure.core.async :as async]
            [clojure.data.json :as json]
            [integrant.core :as ig])
  (:import (java.io InputStream PrintWriter InputStreamReader)
           (java.nio.charset StandardCharsets)))

(defn init-stdio-client!
  [client-req-ch]
  (async/put! client-req-ch {:command    "info"
                             :type       "request"
                             :request-id "0"}))

(defmethod ig/init-key
  :boonmee/stdio-client
  [_ {:keys [client-req-ch client-resp-ch in out]}]
  (init-stdio-client! client-req-ch)
  (let [ts (atom (System/currentTimeMillis))]
    {:ts  ts
     :in  (util/line-handler [line in]
            (try
              (reset! ts (System/currentTimeMillis))
              (let [req (json/read-str line :key-fn keyword)]
                (async/put! client-req-ch req))
              (catch Throwable e
                (async/put! client-resp-ch {:command "error"
                                            :type    "response"
                                            :success false
                                            :message (.getMessage e)}))))
     :out (async/go-loop []
            (when-let [resp (async/<! client-resp-ch)]
              (try
                (.println ^PrintWriter out (json/write-str resp))
                (.flush out)
                (catch Throwable e
                  (async/put! client-resp-ch {:command "error"
                                              :type    "response"
                                              :success false
                                              :message (.getMessage e)})))
              (recur)))}))

(defmethod ig/halt-key!
  :boonmee/stdio-client
  [_ {:keys [in out]}]
  (async/close! out)
  (.close in))

(defmethod ig/init-key :boonmee/stdio-reader
  [_ {:keys [in]}]
  (InputStreamReader. ^InputStream in StandardCharsets/UTF_8))

(defmethod ig/halt-key! :boonmee/stdio-reader
  [_ {:keys [reader]}]
  (.close reader))

(defn config
  [_]
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
                                         :in             (ig/ref :boonmee/stdio-reader)
                                         :out            System/out}
   :boonmee/stdio-reader                {:in System/in}
   :logger/file-logger                  {:fname "boonmee.log"}})