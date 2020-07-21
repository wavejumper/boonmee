(ns boonmee.integration.react-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [boonmee.test-util :refer [with-client request! response!]]
            [boonmee.protocol]))

(def react-resp
  {:command   "completionInfo"
   :data      {:entries                 [{:kind          "const"
                                          :kindModifiers "declare"
                                          :name          "Children"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "cloneElement"
                                          :sortText      "0"}
                                         {:kind          "class"
                                          :kindModifiers "declare"
                                          :name          "Component"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "createContext"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "createElement"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "createFactory"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "createRef"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "forwardRef"
                                          :sortText      "0"}
                                         {:kind          "const"
                                          :kindModifiers "declare"
                                          :name          "Fragment"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "isValidElement"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "lazy"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "memo"
                                          :sortText      "0"}
                                         {:kind          "const"
                                          :kindModifiers "declare"
                                          :name          "Profiler"
                                          :sortText      "0"}
                                         {:kind          "class"
                                          :kindModifiers "declare"
                                          :name          "PureComponent"
                                          :sortText      "0"}
                                         {:kind          "const"
                                          :kindModifiers "declare"
                                          :name          "StrictMode"
                                          :sortText      "0"}
                                         {:kind          "const"
                                          :kindModifiers "declare"
                                          :name          "Suspense"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useCallback"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useContext"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useDebugValue"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useEffect"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useImperativeHandle"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useLayoutEffect"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useMemo"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useReducer"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useRef"
                                          :sortText      "0"}
                                         {:kind          "function"
                                          :kindModifiers "declare"
                                          :name          "useState"
                                          :sortText      "0"}
                                         {:kind          "const"
                                          :kindModifiers "declare"
                                          :name          "version"
                                          :sortText      "0"}]
               :isGlobalCompletion      false
               :isMemberCompletion      true
               :isNewIdentifierLocation false}
   :interop   {:fragments    ['useState]
               :isGlobal     false
               :nextLocation [4 17]
               :prevLocation [4 1]
               :sym          'React
               :usage        :method}
   :requestId "1234567"
   :success   true
   :type      "response"})

(deftest completions--globals
  (with-client [client {:env "node"}]
    (testing "react/useState"
      (let [req {:command   "completions"
                 :type      "request"
                 :requestId "1234567"
                 :arguments {:file        (.getFile (io/resource "react/src/core.cljs"))
                             :projectRoot (.getFile (io/resource "react"))
                             :line        4
                             :offset      12}}]
        (is (s/valid? :client/request req))
        (request! client req)
        (let [resp (response! client 60000)]
          (is (s/valid? :client/response resp))
          (is (= resp react-resp)))))))