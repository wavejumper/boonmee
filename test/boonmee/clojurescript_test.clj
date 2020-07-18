(ns boonmee.clojurescript-test
  (:require [clojure.test :refer :all]
            [boonmee.compiler.analyser :as ana]
            [boonmee.compiler.core :as compiler]))

(defn compile-ns*
  [require-form]
  (->> require-form
       (ana/analyse-npm-require)
       (compiler/compile-es6-require)))

;; https://shadow-cljs.github.io/docs/UsersGuide.html#npm
(deftest analyze-npm-require
  (is (= (compile-ns* '["module-name" :default defaultExport])
         "import defaultExport from 'module-name';"))

  (is (= (compile-ns* '["module-name" :as name])
         "import * as name from 'module-name';"))

  (is (= (compile-ns* '["module-name" :refer (export)])
         "import { export } from 'module-name';"))

  (is (= (compile-ns* '["module-name" :rename {export alias}])
         "import { export as alias } from 'module-name';"))

  (is (= (compile-ns* '["module-name" :refer (export1 export2)])
         "import { export2, export1 } from 'module-name';"))

  (is (= (compile-ns* '["module-name" :as name :default defaultExport])
         "import defaultExport, * as name from 'module-name';"))

  (is (= (compile-ns* '["module-name" :refer (export1) :rename {export2 alias2}])
         "import { export1, export2 as alias2 } from 'module-name';"))

  (is (= (compile-ns* '["module-name"])
         "import 'module-name';"))

  (is (nil? (compile-ns* '[]))))

(def form1
  '((ns foo
      (:require ["react" :as react]))

    (react/useState "xxx")))

(deftest analyse-ctx
  (is (= (select-keys (ana/analyse-string (pr-str form1)) [:npm-deps :npm-syms])
         {:npm-deps '({:package-name "react" :args {:as react}})
          :npm-syms #{'react}})))

(deftest deduce-js-interop
  (testing ":require ['react' ..."
    (is (= {:fragment      nil
            :sym           "react"
            :usage         :require
            :prev-location [1 20]
            :next-location [1 29]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 27]))))

  (testing ":as 'react'"
    (is (= {:fragment      nil
            :sym           'react
            :usage         :unknown
            :prev-location [1 29]
            :next-location [1 42]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 35]))))

  (testing "react/useState"
    (is (= {:fragment      'useState
            :sym           'react
            :usage         :method
            :prev-location [1 42]
            :next-location [1 58]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 45])))))