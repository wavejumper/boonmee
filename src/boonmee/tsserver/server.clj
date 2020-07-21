(ns boonmee.tsserver.server
  (:require [me.raynes.conch.low-level :as sh]
            [clojure.data.json :as json]
            [clojure.core.async :as async]
            [integrant.core :as ig]
            [boonmee.util :as util]
            [boonmee.logging :as log])
  (:import (java.io InputStreamReader InputStream)
           (java.nio.charset StandardCharsets)))

(defmethod ig/init-key :boonmee/tsserver
  [_ {:keys [tsserver-resp-ch tsserver-req-ch tsserver logger]}]
  (let [tsserver-proc (sh/proc tsserver)]
    {:tsserver tsserver-proc
     :out      (util/line-handler [out (InputStreamReader. ^InputStream (:out tsserver-proc) StandardCharsets/UTF_8)]
                 (async/put! tsserver-resp-ch out))
     :err      (util/line-handler [err (InputStreamReader. ^InputStream (:err tsserver-proc) StandardCharsets/UTF_8)]
                 (log/errorf logger "Error from tsserver: %s" err))
     :in       (async/go-loop []
                 (when-let [msg (async/<! tsserver-req-ch)]
                   (sh/feed-from-string tsserver-proc (str (json/write-str msg) \newline))
                   (recur)))}))

(defmethod ig/halt-key! :boonmee/tsserver
  [_ {:keys [tsserver out err in]}]
  (some-> tsserver sh/done)
  (some-> tsserver sh/destroy)
  (some-> in async/close!)
  (.close out)
  (.close err))