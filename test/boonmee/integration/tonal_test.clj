(ns boonmee.integration.tonal-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [boonmee.test-util :refer [with-client request! response!]]
            [boonmee.protocol]))

(deftest malformed-requests)

(deftest completions
  (with-client [client {:env "browser"}]
    (testing "Successful request"
      (let [req {:command   "completions"
                 :type      "request"
                 :requestId "12345"
                 :arguments {:file        (.getFile (io/resource "tonal/src/tonal/core.cljs"))
                             :projectRoot (.getFile (io/resource "tonal/src/tonal"))
                             :line        4
                             :offset      7}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (is (= {:command   "completionInfo"
                  :type      "response"
                  :success   true
                  :data      {:isGlobalCompletion      false
                              :isMemberCompletion      true
                              :isNewIdentifierLocation false
                              :entries                 [{:name          "freqToMidi"
                                                         :kind          "property"
                                                         :kindModifiers "declare"
                                                         :sortText      "0"}
                                                        {:name          "isMidi"
                                                         :kind          "property"
                                                         :kindModifiers "declare"
                                                         :sortText      "0"}
                                                        {:name          "midiToFreq"
                                                         :kind          "property"
                                                         :kindModifiers "declare"
                                                         :sortText      "0"}
                                                        {:name          "midiToNoteName"
                                                         :kind          "property"
                                                         :kindModifiers "declare"
                                                         :sortText      "0"}
                                                        {:name          "toMidi"
                                                         :kind          "property"
                                                         :kindModifiers "declare"
                                                         :sortText      "0"}]}
                  :requestId "12345"}
                 resp)))))

    (testing "Unsuccessful request"

      )))

(deftest quickinfo
  (with-client [client {:env "browser"}]
    (testing "Successful request"
      (let [req {:command   "quickinfo"
                 :type      "request"
                 :requestId "12345"
                 :arguments {:file        (.getFile (io/resource "tonal/src/tonal/core.cljs"))
                             :projectRoot (.getFile (io/resource "tonal/src/tonal"))
                             :line        7
                             :offset      10}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (println resp))))

    (testing "Unsuccessful request"

      ))
  )

(deftest definition
  (with-client [client {:env "browser"}]
    (testing "Successful request"
      (let [req {:command   "definition"
                 :type      "request"
                 :requestId "12345"
                 :arguments {:file        (.getFile (io/resource "tonal/src/tonal/core.cljs"))
                             :projectRoot (.getFile (io/resource "tonal/src/tonal"))
                             :line        7
                             :offset      10}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (println resp))))

    (testing "Unsuccessful request"

      ))
  )