(ns boonmee.api
  (:import (java.util.concurrent.atomic AtomicInteger)))

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
  (tsserver-rpc :open {:file (str file)}))

(defn completions
  [file line offset]
  (tsserver-rpc :completionInfo {:file   (str file)
                                 :line   line
                                 :offset offset
                                 :includeExternalModuleExports true}))