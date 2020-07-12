(ns boonmee.analyzer-test
  (:require [clojure.test :refer :all]
            [boonmee.analyzer :as ana]))

;; https://shadow-cljs.github.io/docs/UsersGuide.html#npm
(deftest analyze-es6-require
  (is (= (ana/analyze-es6-require '["module-name" :default defaultExport])
         {:package-name "module-name"
          :args         '{:default defaultExport}
          :js           "import defaultExport from module-name;"}))

  (is (= (ana/analyze-es6-require '["module-name" :as name])
         {:package-name "module-name"
          :args         '{:as name}
          :js           "import * as name from module-name;"}))

  (is (= (ana/analyze-es6-require '["module-name" :refer (export)])
         {:package-name "module-name"
          :args         '{:refer (export)}
          :js           "import { export } from module-name;"}))

  (is (= (ana/analyze-es6-require '["module-name" :rename {export alias}])
         {:package-name "module-name"
          :args         '{:rename {export alias}}
          :js           "import { export as alias } from module-name;"}))

  (is (= (ana/analyze-es6-require '["module-name" :refer (export1 export2)])
         {:package-name "module-name"
          :args         '{:refer (export1 export2)}
          :js           "import { export2, export1 } from module-name;"}))

  (is (= (ana/analyze-es6-require '["module-name" :as name :default defaultExport])
         {:package-name "module-name",
          :args         '{:default defaultExport, :as name},
          :js           "import defaultExport, * as name from module-name;"}))

  (is (= (ana/analyze-es6-require '["module-name" :refer (export1) :rename {export2 alias2}])
         {:package-name "module-name",
          :args         '{:rename {export2 alias2}, :refer (export1)},
          :js           "import { export1, export2 as alias2 } from module-name;"}))

  (is (= (ana/analyze-es6-require '["module-name"])
         {:package-name "module-name"
          :args         {}
          :js           "import module-name;"})))
