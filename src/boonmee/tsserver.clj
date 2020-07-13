(ns boonmee.tsserver
  (:require [me.raynes.conch.low-level :as sh]
            [cheshire.core :as cheshire]
            [clojure.core.async :as async]
            [integrant.core :as ig]
            [clojure.tools.logging :as log])
  (:import (java.io BufferedReader InputStreamReader)
           (java.nio.charset StandardCharsets)))

(defmacro line-handler
  [[line-binding stream] & body]
  `(async/thread
    ;; TODO: reflection warning
    ;; TODO: closing of reader
    (let [r# (BufferedReader. (InputStreamReader. ~stream StandardCharsets/UTF_8))]
      (doseq [~line-binding (line-seq r#)]
        ~@body))))

(defmethod ig/init-key :boonmee/tsserver
  [_ {:keys [tsserver-resp-ch tsserver-req-ch]}]
  (let [tsserver (sh/proc "tsserver")]
    {:tsserver tsserver
     :out      (line-handler [out (:out tsserver)]
                 (async/put! tsserver-resp-ch out))
     :err      (line-handler [err (:err tsserver)]
                 (log/error err))
     :in       (async/go-loop []
                 (when-let [msg (async/<! tsserver-req-ch)]
                   (log/infof "Incoming message to tsserver: %s" (cheshire/generate-string msg))
                   (sh/feed-from-string tsserver (str (cheshire/generate-string msg) \newline))
                   (recur)))}))

(defmethod ig/halt-key! :boonmee/tsserver
  [_ {:keys [tsserver out err in]}]
  (some-> tsserver sh/done)
  (some-> tsserver sh/destroy)
  (some-> out async/close!)
  (some-> err async/close!)
  (some-> in async/close!))