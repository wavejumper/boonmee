(ns boonmee.client.stdio
  (:require [boonmee.server]
            [boonmee.tsserver.server]
            [boonmee.util :as util]
            [clojure.core.async :as async]
            [clojure.data.json :as json]
            [integrant.core :as ig])
  (:import (java.io InputStream PrintStream PrintWriter InputStreamReader OutputStreamWriter)
           (java.nio.charset StandardCharsets)))

(defprotocol StdOut
  (print-out [this m]))

(extend-protocol StdOut
  PrintWriter
  (print-out [this m]
    (.println this ^String (json/write-str m))
    (.flush this))

  PrintStream
  (print-out [this m]
    (.println this ^String (json/write-str m))
    (.flush this))

  OutputStreamWriter
  (print-out [this m]
    (.write this ^String (json/write-str m))
    (.flush this)))

(defmethod ig/init-key
  :boonmee/stdio-client
  [_ {:keys [client-req-ch client-resp-ch in out]}]
  {:in  (util/line-handler [line in]
          (try
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
              (print-out out resp)
              (catch Throwable e
                (async/put! client-resp-ch {:command "error"
                                            :type    "response"
                                            :success false
                                            :message (.getMessage e)})))
            (recur)))})

(defmethod ig/halt-key! :boonmee/stdio-client
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
  [opts]
  {[:async/chan :chan/tsserver-resp-ch] {}
   [:async/chan :chan/tsserver-req-ch]  {}
   [:async/chan :chan/client-resp-ch]   {}
   [:async/chan :chan/client-req-ch]    {}
   :boonmee/tsserver                    {:tsserver-resp-ch (ig/ref :chan/tsserver-resp-ch)
                                         :tsserver-req-ch  (ig/ref :chan/tsserver-req-ch)
                                         :logger           (ig/ref :logger/file-logger)
                                         :tsserver         (:tsserver opts)}
   :boonmee/server                      {:tsserver-resp-ch (ig/ref :chan/tsserver-resp-ch)
                                         :tsserver-req-ch  (ig/ref :chan/tsserver-req-ch)
                                         :client-req-ch    (ig/ref :chan/client-req-ch)
                                         :client-resp-ch   (ig/ref :chan/client-resp-ch)
                                         :logger           (ig/ref :logger/file-logger)
                                         :ctx              {:client :stdio
                                                            :env    (:env opts)}}
   :boonmee/stdio-client                {:client-req-ch  (ig/ref :chan/client-req-ch)
                                         :client-resp-ch (ig/ref :chan/client-resp-ch)
                                         :in             (ig/ref :boonmee/stdio-reader)
                                         :out            System/out}
   :boonmee/stdio-reader                {:in System/in}
   :logger/file-logger                  {:fname "boonmee.log"}})