(ns boonmee.tsserver.api)

(defn tsserver-rpc
  [seq command arguments]
  {:seq       seq
   :type      "request"
   :command   (name command)
   :arguments arguments})

(defn open
  [id file]
  (tsserver-rpc id :open {:file (str file)}))

(defn completions
  [id file line offset]
  (tsserver-rpc id :completionInfo {:file                         (str file)
                                    :line                         line
                                    :offset                       offset
                                    :includeExternalModuleExports true
                                    :includeInsertTextCompletions true}))

(defn quick-info
  [id file line offset]
  (tsserver-rpc id :quickinfo {:file                         (str file)
                               :line                         line
                               :offset                       offset
                               :includeExternalModuleExports true}))

(defn definition
  [id file line offset]
  (tsserver-rpc id :definition {:file                         (str file)
                                :line                         line
                                :offset                       offset
                                :includeExternalModuleExports true}))

(defn reload
  [id file]
  (tsserver-rpc id :reload {:tmpfile (str file)}))