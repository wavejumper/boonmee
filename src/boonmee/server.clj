(ns boonmee.server
  (:require [integrant.core :as ig]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [expound.alpha :as expound]
            [boonmee.tsserver.api :as api]
            [boonmee.compiler.core :as compiler :refer [es6-import es6-symbol]]
            [boonmee.util :as util]
            [boonmee.logging :as log]
            [boonmee.protocol])
  (:import (java.util.concurrent.atomic AtomicInteger)))

(defn seq-id
  [state]
  (let [seq (:seq state)]
    (.getAndIncrement ^AtomicInteger seq)))

(def ->logger :logger)

(defn handle-definition
  [req]
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

    [(api/open 0 out-file)
     (api/definition 0 out-file js-line js-offset)]))

(defn handle-quick-info
  [req]
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

    [(api/open 0 out-file)
     (api/quick-info 0 out-file js-line js-offset)]))

(defn handle-completions
  [state req]
  (let [id           (seq-id state)
        file         (-> req :arguments :file io/file)
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

    {:tsserver/requests [(api/open id out-file)
                         (api/completions id out-file js-line js-offset)]
     :state             (assoc-in state [:completions id] (:request-id req))}))

(defmulti
 handle-client-request
 (fn [_state req]
   (:command req)))

(defmethod handle-client-request :default
  [state req]
  (log/warnf (->logger state) "Unsupported client request: %s" req)
  {:state state})

(defmethod handle-client-request "info"
  [state req]
  {:state            state
   :client/responses [{:type       "response"
                       :command    "info"
                       :request-id (:request-id req)
                       :success    true
                       :data       {:seq     (let [seq (:seq state)]
                                               (.get ^AtomicInteger seq))
                                    :ctx     (:ctx state)
                                    :init    (:init state)
                                    :version (:version state)}}]})

(defmethod handle-client-request "open"
  [state req]
  #_(handlers/handle-open tsserver-req-ch req)
  {:state state})

(defmethod handle-client-request "completions"
  [state req]
  (handle-completions state req))

(defmethod handle-client-request "quickinfo"
  [state req]
  {:tsserver/requests (handle-quick-info req)
   :state             state})

(defmethod handle-client-request "definition"
  [state req]
  {:tsserver/requests (handle-definition req)
   :state             state})

(defmulti
 handle-tsserver-response
 (fn [_state resp]
   (:command resp)))

(defmethod handle-tsserver-response :default
  [state resp]
  (log/warnf (->logger state) "Unsupported tsserver response: %s" resp)
  {:state state})

(defmethod handle-tsserver-response "completionInfo"
  [state resp]
  (log/info (->logger state) resp)
  (let [seq-id     (get resp "request_seq")
        request-id (get-in state [:completions seq-id])]
    {:client/responses [{:command    "completionInfo"
                         :success    (:success resp)
                         :message    (:message resp)
                         :request-id request-id}]
     :state            (update state :completions dissoc seq-id)}))

(defn parse-tsserver-resp
  [resp]
  (if (or (str/blank? resp) (str/starts-with? resp "Content-Length: "))
    nil
    (json/read-str resp :key-fn keyword)))

(defn initial-state
  [logger ctx]
  {:seq     (AtomicInteger. 0)
   :logger  logger
   :ctx     ctx
   :init    (System/currentTimeMillis)
   :version "1.0.0"})

(defn process-client-req
  [state logger req]
  (try
    (if (s/valid? :client/request req)
      (handle-client-request state req)
      {:state            state
       :client/responses [{:command "error"
                           :type    "response"
                           :success false
                           :message (expound/expound-str :client/request req)}]})
    (catch Throwable e
      (log/errorf logger e "Exception handling client request: %s" req)
      {:state            state
       :client/responses [{:command "error"
                           :type    "response"
                           :success false
                           :message (.getMessage e)}]})))

(defn process-tsserver-resp
  [state logger resp]
  (try
    (if-let [parsed-resp (parse-tsserver-resp resp)]
      (handle-tsserver-response state parsed-resp)
      {:state state})
    (catch Throwable e
      (log/errorf logger e "Exception handling tsserver response: %s" resp)
      {:state            state
       :client/responses [{:command "error"
                           :type    "response"
                           :success false
                           :message (.getMessage e)}]})))

(defn server-loop
  [close-ch {:keys [tsserver-resp-ch tsserver-req-ch client-resp-ch client-req-ch ctx logger]}]
  (async/go-loop [state (initial-state logger ctx)]
    (let [[val ch] (async/alts! [client-req-ch tsserver-resp-ch close-ch])
          result (condp = ch
                   close-ch {:closed? true}
                   client-req-ch (process-client-req state logger val)
                   tsserver-resp-ch (process-tsserver-resp state logger val))]

      (doseq [resp (:client/responses result)]
        ;; Only throw assertion errors on responses if `check-asserts?` is enabled
        #_(s/assert :client/response resp)
        (async/put! client-resp-ch resp))

      (doseq [req (:tsserver/requests result)]
        (async/put! tsserver-req-ch req))

      (cond
        (:closed? result)
        (log/info logger "closing server")

        (:state result)
        (recur (:state result))

        :else
        (do (log/errorf logger "handler returned nil state value, returning previous state: %s" val)
            (recur state))))))

(defmethod ig/init-key :boonmee/server
  [_ opts]
  (let [close-ch (async/chan)]
    {:close-ch    close-ch
     :server-loop (server-loop close-ch opts)}))

(defmethod ig/halt-key! :boonmee/server
  [_ {:keys [server-loop close-ch]}]
  (some-> close-ch async/close!)
  (some-> server-loop async/close!))

(defmethod ig/init-key :async/chan
  [_ _]
  (async/chan))

(defmethod ig/halt-key! :async/chan
  [_ ch]
  (some-> ch async/close!))