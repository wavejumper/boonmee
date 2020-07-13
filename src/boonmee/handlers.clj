(ns boonmee.handlers
  (:require [boonmee.compiler.core :as compiler]
            [boonmee.compiler.dsl :refer [es6-import es6-symbol]]
            [boonmee.api :as api]
            [clojure.java.io :as io]
            [boonmee.util :as util]
            [clojure.core.async :as async]))

(defn handle-open
  [tsserver-req-ch req]
  (let [file         (-> req :arguments :file io/file)
        form         [(es6-import)]
        compiled     (compiler/compile file form)
        project-root (util/project-root file)
        out-file     (util/spit-src project-root compiled)]

    (async/put! tsserver-req-ch (api/open out-file))))

(defn handle-completions
  [tsserver-req-ch req]
  (let [file         (-> req :arguments :file io/file)
        loc          [(-> req :arguments :line)
                      (-> req :arguments :offset)]
        form         [(es6-import)
                      (es6-symbol {:loc     loc
                                   :cursor? true})]
        compiled     (compiler/compile file form)
        project-root (util/project-root file)
        out-file     (util/spit-src project-root compiled)
        js-line      (-> compiled :compiled :line)
        js-offset    (-> compiled :compiled :offset)]

    (async/put! tsserver-req-ch (api/open out-file))
    (async/put! tsserver-req-ch (api/completions out-file js-line js-offset))))

(comment
 (:compiled
  (compiler/compile
   (io/file "examples/tonal/src/tonal/core.cljs")
   [(es6-import)
    (es6-symbol {:loc [4 3] :cursor? true})])))