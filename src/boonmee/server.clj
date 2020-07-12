(ns boonmee.server
  (:require [integrant.core :as ig]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [boonmee.handlers :as handlers]))

(defmulti
 handle-client-request
 (fn [ctx tsserver-req-ch client-resp-ch req]
   (:command req)))

(defmethod handle-client-request :default
  [_ _ _ req]
  (log/warn "Unsupported request " req))

(defmethod handle-client-request "open"
  [_ tsserver-req-ch _ req]
  (handlers/handle-open tsserver-req-ch req))

(defmethod handle-client-request "completions"
  [_ tsserver-req-ch _ req]
  (handlers/handle-completions tsserver-req-ch req))

(defn handle-tsserver-response
  [tsserver-req-ch client-resp-ch resp]
  (log/info "Resp => " resp))

(defmethod ig/init-key :boonmee/server
  [_ {:keys [tsserver-resp-ch tsserver-req-ch
             client-resp-ch client-req-ch ctx]}]
  ;; TODO: close-ch, threadpool etc
  (async/go-loop []
    (async/alt!
     client-req-ch
     ([req]
      (try
        (handle-client-request ctx tsserver-req-ch client-resp-ch req)
        (catch Throwable e
          (log/errorf e "exception handling request" req))))

     tsserver-resp-ch
     ([resp]
      (try
        (handle-tsserver-response tsserver-req-ch client-resp-ch resp)
        (catch Throwable e
          (log/errorf e "exception handling response" resp)))))
    (recur)))

(defmethod ig/halt-key! :boonmee/server
  [_ ch]
  (some-> ch async/close!))