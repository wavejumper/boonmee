(ns boonmee.server
  (:require [integrant.core :as ig]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [boonmee.compiler :as compiler]))

(defmulti
 handle-client-request
 (fn [ctx tsserver-req-ch client-resp-ch req]
   (:command req)))

(defmethod handle-client-request :default
  [_ _ _ req]
  (log/warn "Unsupported request " req))

(defmethod handle-client-request "open"
  [_ _ _ req]
  (try
    (let [file     (-> req :arguments :file)
          compiled (compiler/compile file)]

      )
    (catch Throwable e
      (log/error e "error handling request" req))))


(defn handle-tsserver-response
  [tsserver-req-ch client-resp-ch resp]

  )

(defmethod ig/init-key :boonmee/server
  [_ {:keys [tsserver-resp-ch tsserver-req-ch
             client-resp-ch client-req-ch ctx]}]
  ;; TODO: close-ch, threadpool etc
  (async/go-loop []
    (async/alt!
     client-req-ch
     ([req]
      (handle-client-request ctx tsserver-req-ch client-resp-ch req))

     tsserver-resp-ch
     ([resp]
      (handle-tsserver-response tsserver-req-ch client-resp-ch resp)))
    (recur)))

(defmethod ig/halt-key! :boonmee/server
  [_ ch]
  (some-> ch async/close!))