(ns boonmee.server
  (:require [integrant.core :as ig]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [boonmee.tsserver.api :as api]
            [boonmee.compiler.core :as compiler]
            [boonmee.compiler.dsl :refer [es6-import es6-symbol]]
            [boonmee.util :as util]
            [taoensso.timbre :as timbre]))

(defn handle-definition
  [tsserver-req-ch req]
  (let [file         (-> req :arguments :file io/file)
        loc          [(-> req :arguments :line)
                      (-> req :arguments :offset)]
        form         [(es6-import)
                      (es6-symbol {:loc     loc
                                   :cursor? true})]
        compiled     (compiler/compile file form)
        project-root (util/project-root file)
        out-file     (util/spit-src project-root compiled)
        js-line      (-> compiled :compiled :line)
        js-offset    (-> compiled :compiled :offset)]

    (async/put! tsserver-req-ch (api/open out-file))
    (async/put! tsserver-req-ch (api/definition out-file js-line js-offset))))

(defn handle-quick-info
  [tsserver-req-ch req]
  (let [file         (-> req :arguments :file io/file)
        loc          [(-> req :arguments :line)
                      (-> req :arguments :offset)]
        form         [(es6-import)
                      (es6-symbol {:loc     loc
                                   :cursor? true})]
        compiled     (compiler/compile file form)
        project-root (util/project-root file)
        out-file     (util/spit-src project-root compiled)
        js-line      (-> compiled :compiled :line)
        js-offset    (-> compiled :compiled :offset)]

    (async/put! tsserver-req-ch (api/open out-file))
    (async/put! tsserver-req-ch (api/quick-info out-file js-line js-offset))))

(defn handle-completions
  [tsserver-req-ch req]
  (let [file         (-> req :arguments :file io/file)
        loc          [(-> req :arguments :line)
                      (-> req :arguments :offset)]
        form         [(es6-import)
                      (es6-symbol {:loc     loc
                                   :cursor? true})]
        compiled     (compiler/compile file form)
        project-root (util/project-root file)
        out-file     (util/spit-src project-root compiled)
        js-line      (-> compiled :compiled :line)
        js-offset    (-> compiled :compiled :offset)]

    (async/put! tsserver-req-ch (api/open out-file))
    (async/put! tsserver-req-ch (api/completions out-file js-line js-offset))))

(defmulti
 handle-client-request
 (fn [ctx tsserver-req-ch client-resp-ch req]
   (:command req)))

(defmethod handle-client-request :default
  [_ _ _ req]
  (timbre/warnf "Unsupported client request: %s" req))

(defmethod handle-client-request "open"
  [_ tsserver-req-ch _ req]
  #_(handlers/handle-open tsserver-req-ch req))

(defmethod handle-client-request "completions"
  [_ tsserver-req-ch _ req]
  (handle-completions tsserver-req-ch req))

(defmethod handle-client-request "quickinfo"
  [_ tsserver-req-ch _ req]
  (handle-quick-info tsserver-req-ch req))

(defmethod handle-client-request "definition"
  [_ tsserver-req-ch _ req]
  (handle-definition tsserver-req-ch req))

(defn handle-tsserver-response
  [tsserver-req-ch client-resp-ch resp])

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
          (timbre/errorf e "Exception handling client request: %s" req))))

     tsserver-resp-ch
     ([resp]
      (try
        (handle-tsserver-response tsserver-req-ch client-resp-ch resp)
        (catch Throwable e
          (timbre/errorf e "Exception handling tsserver response: %s" resp)))))
    (recur)))

(defmethod ig/halt-key! :boonmee/server
  [_ ch]
  (some-> ch async/close!))

(defmethod ig/init-key :async/chan
  [_ _]
  (async/chan))

(defmethod ig/halt-key! :async/chan
  [_ ch]
  (some-> ch async/close!))