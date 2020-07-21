(ns boonmee.integration.node-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [boonmee.test-util :refer [with-client request! response!]]
            [boonmee.protocol]))

(def process-resp
  {:command   "completionInfo"
   :data      {:entries                 [{:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "abort"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "addListener"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "allowedNodeEnvironmentFlags"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "arch"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "argv"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "argv0"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "chdir"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "config"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "connected"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "cpuUsage"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "cwd"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "debugPort"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "disconnect"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "domain"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "emit"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "emitWarning"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "env"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "eventNames"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "execArgv"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "execPath"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "exit"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare,optional"
                                          :name          "exitCode"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "features"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "getegid"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "geteuid"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "getgid"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "getgroups"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "getMaxListeners"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "getuid"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "hasUncaughtExceptionCaptureCallback"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "hrtime"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "kill"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "listenerCount"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "listeners"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "memoryUsage"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "nextTick"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "off"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "on"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "once"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "openStdin"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "pid"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "platform"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "ppid"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "prependListener"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "prependOnceListener"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "rawListeners"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "release"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "removeAllListeners"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "removeListener"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare,optional"
                                          :name          "report"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "resourceUsage"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare,optional"
                                          :name          "send"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "setegid"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "seteuid"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "setgid"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "setgroups"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "setMaxListeners"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "setuid"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "setUncaughtExceptionCaptureCallback"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "stderr"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "stdin"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "stdout"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "title"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "umask"
                                          :sortText      "0"}
                                         {:kind          "method"
                                          :kindModifiers "declare"
                                          :name          "uptime"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "version"
                                          :sortText      "0"}
                                         {:kind          "property"
                                          :kindModifiers "declare"
                                          :name          "versions"
                                          :sortText      "0"}]
               :isGlobalCompletion      false
               :isMemberCompletion      true
               :isNewIdentifierLocation false}
   :interop   {:fragments    ['process 'version]
               :isGlobal     true
               :nextLocation [1 12]
               :prevLocation [1 1]
               :sym          'js
               :usage        :property}
   :requestId "1234567"
   :success   true
   :type      "response"})

(deftest completions--globals
  (with-client [client {:env "node"}]
    (testing "globals (js/process ...)"
      (let [req {:command   "completions"
                 :type      "request"
                 :requestId "1234567"
                 :arguments {:file        (.getFile (io/resource "node/src/core.cljs"))
                             :projectRoot (.getFile (io/resource "node"))
                             :line        1
                             :offset      8}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 60000)]
          (is (s/valid? :client/response resp))
          (is (= resp process-resp)))))))