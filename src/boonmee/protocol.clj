(ns boonmee.protocol
  (:require [clojure.spec.alpha :as s]))

;;;; Client requests

(defmulti client-request :command)

(s/def :client/request
  (s/multi-spec client-request :command))

(s/def :client.request/request-id string?)
(s/def :client.request/type #{"request"})

;;; Info

(s/def :client.request.info/command #{"info"})

(defmethod client-request "info" [_]
  (s/keys :req-un [:client.request/request-id
                   :client.request/type
                   :client.request.info/command]))

;;; Completions request

(s/def :client.request.completions/command #{"completions"})
(s/def :client.request.completions.arguments/file string?)
(s/def :client.request.completions.arguments/line nat-int?)
(s/def :client.request.completions.arguments/offset nat-int?)

(s/def :client.request.completions/arguments
  (s/keys :req-un [:client.request.completions.arguments/file
                   :client.request.completions.arguments/line
                   :client.request.completions.arguments/offset]))

(defmethod client-request "completions" [_]
  (s/keys :req-un [:client.request/request-id
                   :client.request/type
                   :client.request.completions/command
                   :client.request.completions/arguments]))

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
                   :client.response/message
                   :client.request/request-id
                   :client.response.info/command
                   :client.response.info/data]))

;;; Error response

(s/def :client.response.error/command #{"error"})

(defmethod client-response "error" [_]
  (s/keys :req-un [:client.response/type
                   :client.response/success
                   :client.response.error/command]
          :opt-un [:client.response/message
                   :client.request/request-id]))

;;; Completion info

(s/def :client.response.completionInfo/command #{"completionInfo"})
(s/def :client.response.completionInfo/data string?)

(defmethod client-response "completionInfo" [_]
  (s/keys :req-un [:client.response/type
                   :client.response/success
                   :client.response.completionInfo/command]
          :opt-un [ :client.request/request-id
                   :client.response.completionInfo/data
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