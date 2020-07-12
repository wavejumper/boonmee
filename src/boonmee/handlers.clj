(ns boonmee.handlers
  (:require [boonmee.compiler :as compiler]
            [boonmee.api :as api]
            [clojure.java.io :as io]
            [boonmee.util :as util]
            [clojure.core.async :as async]))

(defn handle-open
  [tsserver-req-ch req]
  (let [file         (-> req :arguments :file io/file)
        compiled     (compiler/compile file)
        project-root (util/project-root file)
        out-file     (util/spit-src project-root compiled)]
    (async/put! tsserver-req-ch (api/open out-file))))

(defn handle-completions
  [tsserver-req-ch req]
  (let [file         (-> req :arguments :file io/file)
        compiled     (compiler/compile file)
        project-root (util/project-root file)
        out-file     (util/spit-src project-root compiled)]
    (async/put! tsserver-req-ch (api/completions out-file 2 10))))