(ns boonmee.tsserver
  (:require [me.raynes.conch.low-level :as sh]
            [cheshire.core :as cheshire]
            [clojure.core.async :as async]
            [integrant.core :as ig]
            [clojure.tools.logging :as log])
  (:import (java.io BufferedReader InputStreamReader)
           (java.nio.charset StandardCharsets)
           (java.util.concurrent.atomic AtomicInteger)))

(defonce tsserver-seq
  (AtomicInteger. 0))

(defn tsserver-rpc
  [command arguments]
  {:seq       (.getAndIncrement tsserver-seq)
   :type      "request"
   :command   (name command)
   :arguments arguments})

(defn open
  [file]
  (tsserver-rpc :open {:file file}))

(defn completions
  [file]
  (tsserver-rpc :completionInfo {:file   file
                                 :line   3
                                 :offset 5}))

(defmacro line-handler
  [[line-binding stream] & body]
  `(async/thread
    ;; TODO: reflection warning
    ;; TODO: closing of reader
    (let [r# (BufferedReader. (InputStreamReader.  ~stream StandardCharsets/UTF_8))]
      (doseq [~line-binding (line-seq r#)]
        ~@body))))

(defmethod ig/init-key ::rpc
  [_ {:keys [handler-ch in-ch]}]
  (let [tsserver (sh/proc "tsserver")]
    {:tsserver tsserver
     :out      (line-handler [out (:out tsserver)]
                 (async/put! handler-ch out))
     :err      (line-handler [err (:err tsserver)]
                 (log/error err))
     :in       (async/go-loop []
                 (when-let [msg (async/<! in-ch)]
                   (log/debugf "Incoming message to tsserver: %s" (cheshire/generate-string msg))
                   (sh/feed-from-string tsserver (str (cheshire/generate-string msg) \newline))
                   (recur)))}))

(defmethod ig/halt-key! ::rpc
  [_ {:keys [tsserver out err in]}]
  (some-> tsserver sh/done)
  (some-> tsserver sh/destroy)
  (some-> out async/close!)
  (some-> err async/close!)
  (some-> in async/close!))