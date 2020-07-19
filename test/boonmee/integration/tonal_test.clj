(ns boonmee.integration.tonal-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [boonmee.test-util :refer [with-client request! response!]]
            [boonmee.protocol]))

(deftest info
  (with-client [client {:env "browser"}]
    (testing "Successful request"
      (let [req {:command   "info"
                 :type      "request"
                 :requestId "12345"}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (is (= resp
                 {:type      "response"
                  :command   "info"
                  :requestId "12345"
                  :success   true
                  :data      {:seq     0
                              :ctx     {:client :clojure
                                        :env    "browser"}
                              :init    (-> resp :data :init)
                              :version "1.0.0"}})))))))

(deftest malformed-requests)

(deftest completions
  (with-client [client {:env "browser"}]
    (testing "Successful request"
      (let [req {:command   "completions"
                 :type      "request"
                 :requestId "12345"
                 :arguments {:file        (.getFile (io/resource "tonal/src/tonal/core.cljs"))
                             :projectRoot (.getFile (io/resource "tonal"))
                             :line        4
                             :offset      7}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (is (= {:command   "completionInfo"
                  :type      "response"
                  :success   true
                  :interop   {:fragments     ['m]
                              :global?       false
                              :prev-location [4 1]
                              :next-location [7 1]
                              :sym           'Midi
                              :usage         :method}
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



    (testing "Unsuccessful request (no interop at loc)"
      (let [req {:command   "completions"
                 :type      "request"
                 :requestId "123456"
                 :arguments {:file        (.getFile (io/resource "tonal/src/tonal/core.cljs"))
                             :projectRoot (.getFile (io/resource "tonal"))
                             :line        12
                             :offset      1}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (is (= resp
                 {:command   "completionInfo"
                  :type      "response"
                  :success   false
                  :interop   nil
                  :requestId "123456"
                  :message   "No interop found at [12 1]"})))))))



(deftest quickinfo
  (with-client [client {:env "browser"}]
    (testing "Successful request"
      (let [req {:command   "quickinfo"
                 :type      "request"
                 :requestId "12345"
                 :arguments {:file        (.getFile (io/resource "tonal/src/tonal/core.cljs"))
                             :projectRoot (.getFile (io/resource "tonal"))
                             :line        7
                             :offset      10}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (is (= {:command "quickinfo"
                  :type "response"
                  :success true
                  :data {:kind "property"
                         :kindModifiers "declare"
                         :start {:line 2, :offset 6}
                         :end {:line 2, :offset 16}
                         :displayString "(property) midiToFreq: (midi: number, tuning?: number) => number"
                         :documentation ""
                         :tags []}
                  :interop {:fragments ['midiToFreq]
                            :sym 'Midi
                            :global? false
                            :usage :method
                            :prev-location [7 1]
                            :next-location [7 18]}
                  :requestId "12345"}
                 resp)))))

    (testing "Unsuccessful request (no interop at loc)"
      (let [req {:command   "quickinfo"
                 :type      "request"
                 :requestId "123456"
                 :arguments {:file        (.getFile (io/resource "tonal/src/tonal/core.cljs"))
                             :projectRoot (.getFile (io/resource "tonal"))
                             :line        4
                             :offset      1}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (is (= {:command   "quickinfo"
                  :type      "response"
                  :success   false
                  :interop   nil
                  :requestId "123456"
                  :message   "No content available."}
                 resp)))))))

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

    (testing "Unsuccessful request (no interop at loc)"
      (let [req {:command   "definition"
                 :type      "request"
                 :requestId "123456"
                 :arguments {:file        (.getFile (io/resource "tonal/src/tonal/core.cljs"))
                             :projectRoot (.getFile (io/resource "tonal"))
                             :line        4
                             :offset      1}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (println resp)))
      ))
  )