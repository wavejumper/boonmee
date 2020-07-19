(ns boonmee.server
  (:require [integrant.core :as ig]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [expound.alpha :as expound]
            [boonmee.tsserver.api :as tsserver.api]
            [boonmee.compiler.core :as compiler :refer [es6-import es6-symbol]]
            [boonmee.util :as util]
            [boonmee.logging :as log]
            [boonmee.protocol])
  (:import (java.io File)
           (java.util.concurrent.atomic AtomicInteger)))

(defn seq-id
  [state]
  (let [seq (:seq state)]
    (.getAndIncrement ^AtomicInteger seq)))

(def ->logger :logger)

(defn env [state]
  (-> state :ctx :env))

(defn camel-case-interop
  [interop]
  (when interop
    {:fragments    (:fragments interop)
     :isGlobal     (:global? interop)
     :prevLocation (:prev-location interop)
     :nextLocation (:next-location interop)
     :sym          (:sym interop)
     :usage        (:usage interop)}))

(defn handle-definition
  [state req]
  (let [id           (seq-id state)
        file         (-> req :arguments :file io/file)
        project-root (-> req :arguments :projectRoot io/file)
        loc          [(-> req :arguments :line)
                      (-> req :arguments :offset)]
        form         [(es6-import)
                      (es6-symbol {:loc     loc
                                   :cursor? true})]
        compiled     (compiler/compile (env state) file form)
        out-file     (util/spit-src project-root compiled)
        js-line      (-> compiled :compiled :line)
        js-offset    (-> compiled :compiled :offset)]

    {:tsserver/requests [(tsserver.api/open 0 out-file)
                         (tsserver.api/definition 0 out-file js-line js-offset)]
     :state             (-> state
                            (assoc-in [:definition id] {:req req :compiled compiled})
                            (update :files conj out-file))}))

(defn handle-quick-info
  [state req]
  (let [id           (seq-id state)
        file         (-> req :arguments :file io/file)
        loc          [(-> req :arguments :line)
                      (-> req :arguments :offset)]
        form         [(es6-import)
                      (es6-symbol {:loc     loc
                                   :cursor? true})]
        compiled     (compiler/compile (env state) file form)
        project-root (-> req :arguments :projectRoot io/file)
        out-file     (util/spit-src project-root compiled)
        js-line      (-> compiled :compiled :line)
        js-offset    (-> compiled :compiled :offset)]

    {:tsserver/requests [(tsserver.api/open id out-file)
                         (tsserver.api/quick-info id out-file js-line js-offset)]
     :state             (-> state
                            (assoc-in [:quickinfo id] {:req req :compiled compiled})
                            (update :files conj out-file))}))

(defn handle-completions
  [state req]
  (let [id       (seq-id state)
        file     (-> req :arguments :file io/file)
        loc      [(-> req :arguments :line)
                  (-> req :arguments :offset)]
        form     [(es6-import)
                  (es6-symbol {:loc     loc
                               :cursor? true})]
        compiled (compiler/compile (env state) file form)]
    (if (-> compiled :compiled :cursor :sym)
      (let [project-root (-> req :arguments :projectRoot io/file)
            out-file     (util/spit-src project-root compiled)
            js-line      (-> compiled :compiled :line)
            js-offset    (-> compiled :compiled :offset)]
        {:tsserver/requests [(tsserver.api/open id out-file)
                             (tsserver.api/completions id out-file js-line js-offset)]
         :state             (-> state
                                (assoc-in [:completions id] {:req req :compiled compiled})
                                (update :files conj out-file))})

      {:state            state
       :client/responses [{:command   "completionInfo"
                           :type      "response"
                           :success   false
                           :interop   nil
                           :requestId (:requestId req)
                           :message   (str "No interop found at " loc)}]})))

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
   :client/responses [{:type      "response"
                       :command   "info"
                       :requestId (:requestId req)
                       :success   true
                       :data      {:seq     (let [seq (:seq state)]
                                              (.get ^AtomicInteger seq))
                                   :ctx     (:ctx state)
                                   :init    (:init state)
                                   :version (:version state)}}]})

(defmethod handle-client-request "open"
  [state _]
  {:state state})

(defmethod handle-client-request "heartbeat"
  [state _]
  {:state state})

(defmethod handle-client-request "flush"
  [state _]
  (let [files (->> (:files state)
                   (filter (fn [^File f]
                             (str/ends-with? (.getName f) ".ts"))))
        reqs  (map #(tsserver.api/close (seq-id state) %) files)]
    (doseq [f files]
      (log/debugf (->logger state) "Deleting %s" f)
      (io/delete-file f :silently true))
    {:state             (update state :files #(remove (set files) %))
     :tsserver/requests reqs}))

(defmethod handle-client-request "completions"
  [state req]
  (handle-completions state req))

(defmethod handle-client-request "quickinfo"
  [state req]
  (handle-quick-info state req))

(defmethod handle-client-request "definition"
  [state req]
  (handle-definition state req))

(defmulti
 handle-tsserver-response
 (fn [_state resp]
   (case (:type resp)
     "event" [(:type resp) (:event resp)]
     "response" [(:type resp) (:command resp)]
     nil)))

(defmethod handle-tsserver-response :default
  [state resp]
  (log/warnf (->logger state) "Unsupported tsserver response: %s" resp)
  {:state state})

(defmethod handle-tsserver-response ["event" "typingsInstallerPid"]
  [state _]
  {:state state})

(defmethod handle-tsserver-response ["response" "completionInfo"]
  [state resp]
  (let [seq-id     (:request_seq resp)
        request-id (get-in state [:completions seq-id :req :requestId])
        interop    (get-in state [:completions seq-id :compiled :compiled :cursor :sym])
        message    (:message resp)
        data       (:body resp)]
    {:client/responses [(cond-> {:command   "completionInfo"
                                 :type      "response"
                                 :success   (:success resp)
                                 :interop   (camel-case-interop interop)
                                 :requestId request-id}
                          message (assoc :message message)
                          data (assoc :data data))]
     :state            (update state :completions dissoc seq-id)}))

(defmethod handle-tsserver-response ["response" "quickinfo"]
  [state resp]
  (let [seq-id     (:request_seq resp)
        request-id (get-in state [:quickinfo seq-id :req :requestId])
        interop    (get-in state [:quickinfo seq-id :compiled :compiled :cursor :sym])
        message    (:message resp)
        data       (:body resp)]
    {:client/responses [(cond-> {:command   "quickinfo"
                                 :type      "response"
                                 :success   (:success resp)
                                 :interop   (camel-case-interop interop)
                                 :requestId request-id}
                          message (assoc :message message)
                          data (assoc :data (-> data (dissoc :start) (dissoc :end))))]
     :state            (update state :quickinfo dissoc seq-id)}))

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
   :version "1.0.0"
   :files   #{}})

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