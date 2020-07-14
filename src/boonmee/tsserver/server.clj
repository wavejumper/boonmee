(ns boonmee.tsserver.server
  (:require [me.raynes.conch.low-level :as sh]
            [cheshire.core :as cheshire]
            [clojure.core.async :as async]
            [integrant.core :as ig]
            [boonmee.util :as util])
  (:import (java.io InputStreamReader)
           (java.nio.charset StandardCharsets)))

(defmethod ig/init-key :boonmee/tsserver
  [_ {:keys [tsserver-resp-ch tsserver-req-ch]}]
  (let [tsserver (sh/proc "tsserver")]
    {:tsserver tsserver
     :out      (util/line-handler [out (InputStreamReader. (:out tsserver) StandardCharsets/UTF_8)]
                 (async/put! tsserver-resp-ch out))
     :err      (util/line-handler [err (InputStreamReader. (:err tsserver) StandardCharsets/UTF_8)]
                 (println err))
     :in       (async/go-loop []
                 (when-let [msg (async/<! tsserver-req-ch)]
                   (println "Incoming message to tsserver: %s" (cheshire/generate-string msg))
                   (sh/feed-from-string tsserver (str (cheshire/generate-string msg) \newline))
                   (recur)))}))

(defmethod ig/halt-key! :boonmee/tsserver
  [_ {:keys [tsserver out err in]}]
  (some-> tsserver sh/done)
  (some-> tsserver sh/destroy)
  (some-> out async/close!)
  (some-> err async/close!)
  (some-> in async/close!))