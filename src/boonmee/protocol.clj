(ns boonmee.protocol
  (:require [clojure.spec.alpha :as s]))

;;;; Client requests

(defmulti client-request :command)

(s/def :client/request
  (s/multi-spec client-request :command))

(s/def :client.request/requestId string?)
(s/def :client.request/type #{"request"})

;;; Heartbeat
;;;
;;; A heartbeat is required for the TCP protocol (at a defined heartbeat-ms, default 30s)
;;; You can send this client request to keep the TCP connection open

(s/def :client.request.heartbeat/command #{"heartbeat"})

(defmethod client-request "heartbeat" [_]
  (s/keys :req-un [:client.request/requestId
                   :client.request/type
                   :client.request.heartbeat/command]))

;;; Info
;;;
;;; Returns info relating to the boonmee instance + connection

(s/def :client.request.info/command #{"info"})

(defmethod client-request "info" [_]
  (s/keys :req-un [:client.request/requestId
                   :client.request/type
                   :client.request.info/command]))

;;; Completions request

(s/def :client.request.completions/command #{"completions"})
(s/def :client.request.completions.arguments/file string?)
(s/def :client.request.completions.arguments/projectRoot string?)
(s/def :client.request.completions.arguments/line nat-int?)
(s/def :client.request.completions.arguments/offset nat-int?)

(s/def :client.request.completions/arguments
  (s/keys :req-un [:client.request.completions.arguments/file
                   :client.request.completions.arguments/projectRoot
                   :client.request.completions.arguments/line
                   :client.request.completions.arguments/offset]))

(defmethod client-request "completions" [_]
  (s/keys :req-un [:client.request/requestId
                   :client.request/type
                   :client.request.completions/command
                   :client.request.completions/arguments]))

;;; Quickinfo request

(s/def :client.request.quickinfo/command #{"quickinfo"})

(defmethod client-request "quickinfo" [_]
  (s/keys :req-un [:client.request/requestId
                   :client.request/type
                   :client.request.quickinfo/command
                   :client.request.completions/arguments]))

;;; Definition request

(s/def :client.request.definition/command #{"definition"})

(defmethod client-request "definition" [_]
  (s/keys :req-un [:client.request/requestId
                   :client.request/type
                   :client.request.definition/command
                   :client.request.completions/arguments]))

;;; Flush
;;;
;;; Flushes (eg, cleans files in .boonmee tmp dir) + closes any open files on running tsserver

(s/def :client.request.flush/command #{"flush"})

(defmethod client-request "flush" [_]
  (s/keys :req-un [:client.request/requestId
                   :client.request/type
                   :client.request.flush/command]))

;;;; Client responses

(defmulti client-response :command)

(s/def :client/response
  (s/multi-spec client-response :command))

(s/def :client.response/type #{"response"})
(s/def :client.response/success boolean?)
(s/def :client.response/message string?)

;;; Info

(s/def :client.response.info/command #{"info"})

(s/def :client.response.info.data/init nat-int?)
(s/def :client.response.info.data/seq nat-int?)
(s/def :client.response.info.data/version string?)

(s/def :client.response.info/data
  (s/keys :req-un [:client.response.info.data/init
                   :client.response.info.data/seq
                   :client.response.info.data/version]))

(defmethod client-response "info" [_]
  (s/keys :req-un [:client.response/type
                   :client.response/success
                   :client.request/requestId
                   :client.response.info/command
                   :client.response.info/data]))

;;; Error response

(s/def :client.response.error/command #{"error"})

(defmethod client-response "error" [_]
  (s/keys :req-un [:client.response/type
                   :client.response/success
                   :client.response.error/command]
          :opt-un [:client.response/message
                   :client.request/requestId]))

;;; Completion info

(s/def :client.response.completionInfo/command #{"completionInfo"})
(s/def :client.response.completionInfo/data string?)
;; TODO: write spec
(s/def :client.response.completionInfo/data map?)
(s/def :client.response.completionInfo/interop (s/nilable map?))

(defmethod client-response "completionInfo" [_]
  (s/keys :req-un [:client.response/type
                   :client.response/success
                   :client.response.completionInfo/command]
          :opt-un [:client.request/requestId
                   :client.response.completionInfo/data
                   :client.response.completionInfo/interop
                   :client.response/message]))

(comment
 (s/explain-str
  :client/request
  {:command    "completions"
   :type       "request"
   :request-id "foo"
   :arguments  {:file   "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal/src/tonal/core.cljs"
                :line   6
                :offset 6}}))