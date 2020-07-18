(ns boonmee.tsserver-integration-test
  (:require [clojure.test :refer :all]
            [boonmee.client.clojure :as client]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [boonmee.protocol]
            [clojure.spec.alpha :as s]))

;;;; Test utils

(defn clean-up-boonmee
  [opts]
  (println "Clean up!"))

(defmacro with-client [[sym opts] & body]
  `(let [opts#   ~opts
         client# (client/start (client/map->ClojureClient {:config (client/config {})}))]
    (try
      (let [~sym client#]
        ~@body)
      (finally
        (client/stop client#)
        (clean-up-boonmee opts#)))))

(defn response!
  [client timeout-ms]
  (let [timeout-ch (async/timeout timeout-ms)
        [val ch] (async/alts!! [(:resp-ch client) timeout-ch])]
    (when-not (= ch timeout-ch)
      val)))

(defn request!
  [client msg]
  (async/put! (:req-ch client) msg))

;;;; Integration tests

(deftest malformed-requests)

(deftest completions
  (with-client [client {}]
    (testing "Successful request"
      (let [req {:command    "completions"
                 :type       "request"
                 :request-id "12345"
                 :arguments  {:file   (.getFile (io/resource "tonal/src/tonal/core.cljs"))
                              :line   4
                              :offset 7}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (println resp)
          (is (s/valid? :client/response resp))
          (is (map? resp)))))

    (testing "Unsuccessful request"

      )))