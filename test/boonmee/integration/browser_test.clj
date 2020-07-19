(ns boonmee.integration.browser-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [boonmee.test-util :refer [with-client request! response!]]
            [boonmee.protocol]))

(def browser-resp
  {:command   "completionInfo"
   :data      {:entries                 [{:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "ancestorOrigins"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "assign"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "hash"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "host"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "hostname"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "href"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "origin"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "pathname"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "port"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "protocol"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "reload"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "replace"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "search"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "toString"
                                          :sortText      "0"}]
               :isGlobalCompletion      false
               :isMemberCompletion      true
               :isNewIdentifierLocation false}
   :interop   {:fragments     ['location 'href]
               :global?       true
               :next-location [1 9]
               :prev-location [1 1]
               :sym           'js
               :usage         :property}
   :requestId "1234567"
   :success   true
   :type      "response"})

(deftest completions--globals
  (with-client [client {:env "browser"}]
    (testing "globals (js/Document ...)"
      (let [req {:command   "completions"
                 :type      "request"
                 :requestId "1234567"
                 :arguments {:file        (.getFile (io/resource "browser/src/core.cljs"))
                             :projectRoot (.getFile (io/resource "browser"))
                             :line        1
                             :offset      3}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 10000)]
          (is (s/valid? :client/response resp))
          (is (= resp browser-resp)))))))