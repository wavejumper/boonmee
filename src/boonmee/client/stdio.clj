(ns boonmee.client.stdio
  (:require [boonmee.server]
            [boonmee.tsserver.server]
            [boonmee.util :as util]
            [clojure.java.io :as io]
            [clojure.core.async :as async]
            [cheshire.core :as cheshire]
            [integrant.core :as ig]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders])
  (:gen-class))

(defn init-stdio-client!
  [client-req-ch])

(defmethod ig/init-key
  :boonmee/stdio-client
  [_ {:keys [client-req-ch client-resp-ch in out]}]
  (init-stdio-client! client-req-ch)
  {:in  (util/line-handler [line in]
          (try
            (let [req (cheshire/parse-string-strict line)]
              (async/put! client-req-ch req))
            (catch Throwable e
              (timbre/errorf e "Failed to parse req %s" line))))
   :out (async/go-loop []
          (when-let [resp (async/<! client-resp-ch)]
            (print-dup (cheshire/generate-string resp) out)
            (recur)))})

(defmethod ig/halt-key!
  :boonmee/stdio-client
  [_ {:keys [in]}]
  (some-> in async/close!))

(defmethod ig/init-key :boonmee/stdio-logger
  [_ {:keys [fname enabled? level]}]
  (timbre/merge-config!
   {:level     level
    :appenders {:spit    (appenders/spit-appender {:fname fname :enabled? enabled?})
                :println {:enabled? false}}}))

(defn config []
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
                                         :in             *in*
                                         :out            *out*}
   :boonmee/stdio-logger                {:fname    (io/file (System/getProperty "java.io.tmpdir") "boonmee.log")
                                         :enabled? true
                                         :level    :info}})

(defn -main [& _]
  (ig/init (config)))