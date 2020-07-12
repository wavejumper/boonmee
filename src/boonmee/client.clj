(ns boonmee.client
  (:require [integrant.core :as ig]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]))

(defmethod ig/init-key
  :boonmee/client
  [_ {:keys [client-req-ch client-resp-ch]}]
  {:out (async/go-loop []
          (when-let [resp (async/<! client-resp-ch)]
            (log/info resp)
            (recur)))
   :in  client-req-ch})

(defmethod ig/halt-key!
  :boonmee/client
  [_ {:keys [out]}]
  (some-> out async/close!))