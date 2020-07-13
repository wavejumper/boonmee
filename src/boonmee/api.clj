(ns boonmee.api)

(defn tsserver-rpc
  [command arguments]
  {:seq       0
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

(defn reload
  [file]
  (tsserver-rpc :reload {:tmpfile (str file)}))