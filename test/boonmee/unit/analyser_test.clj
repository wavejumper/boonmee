(ns boonmee.unit.analyser-test
  (:require [clojure.test :refer :all]
            [boonmee.compiler.analyser :as ana]
            [boonmee.compiler.core :as compiler]
            [boonmee.test-util :as tu]))

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

    ;; method call
    (react/useState "xxx")

    ;; method call
    (.useState react "xxx")

    ;; constructor
    (js/Docuemnt. "foo")

    ;; method call
    (js/Document.adoptNode)

    ;; method call
    (.adoptNode js/Document)

    ;; get property
    (aget js/Document "head")

    ;; get property
    (.-head js/Document)

    ;; set property
    (aset js/Document "head" "foo")

    ;; method call outside of form
    react/createElement))

(comment
 (tu/locations form1))

(deftest analyse-ctx
  (is (= (select-keys (ana/analyse-string (pr-str form1)) [:npm-deps :npm-syms])
         {:npm-deps '({:package-name "react" :args {:as react}})
          :npm-syms #{'react 'js}})))

(deftest deduce-js-interop
  (testing ":require ['react' ..."
    (is (= {:fragments     nil
            :sym           "react"
            :usage         :require
            :global?       false
            :prev-location [1 20]
            :next-location [1 29]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 27]))))

  (testing ":as 'react'"
    (is (= {:fragments     nil
            :sym           'react
            :usage         :require
            :global?       false
            :prev-location [1 29]
            :next-location [1 42]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 35]))))

  (testing "react/useState"
    (is (= {:fragments     ['useState]
            :sym           'react
            :usage         :method
            :global?       false
            :prev-location [1 42]
            :next-location [1 58]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 45]))))

  (testing "(.useState react ...)"
    (is (= {:fragments     '[useState]
            :sym           'react
            :global?       false
            :usage         :method
            :prev-location [1 65]
            :next-location [1 76]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 67]))))

  (testing "(js/Document. ...)"
    (is (= {:fragments     ['Docuemnt]
            :global?       true
            :sym           'js
            :usage         :constructor
            :prev-location [1 82]
            :next-location [1 90]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 90]))))

  (testing "(js/Document.adoptNode)"
    (is (= {:fragments     ['Document 'adoptNode]
            :global?       true
            :sym           'js
            :usage         :method
            :prev-location [1 110]
            :next-location [1 134]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 117]))))

  (testing "(.adoptNode js/Document)"
    (is (= {:fragments     '[Document adoptNode]
            :sym           'js
            :global?       true
            :usage         :method
            :prev-location [1 111]
            :next-location [1 135]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 135])))

    (is (= {:fragments     '[Document adoptNode]
            :sym           'js
            :global?       true
            :usage         :method
            :prev-location [1 135]
            :next-location [1 159]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 147]))))

  (testing "(aget js/Document head)"
    (testing "aget"
      (is (= {:fragments     ['Document 'head]
              :sym           'js
              :global?       true
              :usage         :property
              :prev-location [1 146]
              :next-location [1 160]}
             (ana/deduce-js-interop
              (ana/analyse-string (pr-str form1))
              [1 160]))))

    (testing "js/Document"
      (is (= {:fragments     ['Document 'head]
              :sym           'js
              :global?       true
              :usage         :property
              :prev-location [1 159]
              :next-location [1 165]}
             (ana/deduce-js-interop
              (ana/analyse-string (pr-str form1))
              [1 165]))))

    (testing "head"
      (is (= {:fragments     ['Document 'head]
              :sym           'js
              :global?       true
              :usage         :property
              :prev-location [1 160]
              :next-location [1 177]}
             (ana/deduce-js-interop
              (ana/analyse-string (pr-str form1))
              [1 177])))))

  (testing "(.-head js/Dcoument)"
    (is (= {:fragments ['Document 'head]
            :sym 'js
            :global? true
            :usage :property
            :prev-location [1 185]
            :next-location [1 193]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 187]))))

  (testing "(aset js/Document head)"
    (is (= {:fragments     ['Document 'head]
            :sym           'js
            :global?       true
            :usage         :property
            :prev-location [1 193]
            :next-location [1 207]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 207]))))

  (testing "method called outside of form"
    (is (= {:fragments     ['createElement]
            :sym           'react
            :global?       false
            :usage         :property
            :prev-location [1 231]
            :next-location [1 238]}
           (ana/deduce-js-interop
            (ana/analyse-string (pr-str form1))
            [1 239])))))