(ns boonmee.tsserver.api
  (:import (java.io File)))

(defn tsserver-rpc
  [seq command arguments]
  {:seq       seq
   :type      "request"
   :command   (name command)
   :arguments arguments})

(defn open
  [id ^File file]
  (tsserver-rpc id :open {:file (str (.getAbsoluteFile file))}))

(defn close
  [id ^File file]
  (tsserver-rpc id :close {:file (str (.getAbsoluteFile file))}))

(defn completions
  [id ^File file line offset]
  (tsserver-rpc id :completionInfo {:file                         (str (.getAbsoluteFile file))
                                    :line                         line
                                    :offset                       offset
                                    :includeExternalModuleExports true
                                    :includeInsertTextCompletions true}))

(defn quick-info
  [id ^File file line offset]
  (tsserver-rpc id :quickinfo {:file                         (str (.getAbsoluteFile file))
                               :line                         line
                               :offset                       offset
                               :includeExternalModuleExports true}))

(defn definition
  [id ^File file line offset]
  (tsserver-rpc id :definition {:file                         (str (.getAbsoluteFile file))
                                :line                         line
                                :offset                       offset
                                :includeExternalModuleExports true}))

(defn reload
  [id ^File file]
  (tsserver-rpc id :reload {:tmpfile (str (.getAbsoluteFile file))}))