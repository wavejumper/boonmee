(ns boonmee.analyzer-test
  (:require [clojure.test :refer :all]
            [boonmee.compiler :as compiler]))

;; https://shadow-cljs.github.io/docs/UsersGuide.html#npm
(deftest analyze-es6-require
  (is (= (compiler/compile-es6-require '["module-name" :default defaultExport])
         "import defaultExport from module-name;"))

  (is (= (compiler/compile-es6-require '["module-name" :as name])
         "import * as name from module-name;"))

  (is (= (compiler/compile-es6-require '["module-name" :refer (export)])
         "import { export } from module-name;"))

  (is (= (compiler/compile-es6-require '["module-name" :rename {export alias}])
         "import { export as alias } from module-name;"))

  (is (= (compiler/compile-es6-require '["module-name" :refer (export1 export2)])
         "import { export2, export1 } from module-name;"))

  (is (= (compiler/compile-es6-require '["module-name" :as name :default defaultExport])
         "import defaultExport, * as name from module-name;"))

  (is (= (compiler/compile-es6-require '["module-name" :refer (export1) :rename {export2 alias2}])
         "import { export1, export2 as alias2 } from module-name;"))

  (is (= (compiler/compile-es6-require '["module-name"])
         "import module-name;")))
