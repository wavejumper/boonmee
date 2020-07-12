(ns boonmee.util
  (:require [clojure.java.io :as io])
  (:import (java.io File)
           (java.security MessageDigest)))

(defn sha256
  [string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))

(defn project-root
  [^File file]
  (when (and file (.exists file))
    (if (.exists (io/file file "package.json"))
      (io/file file)
      (when-let [parent (.getParent file)]
        (recur (io/file parent))))))

(defn spit-src
  [^File project-root {:keys [hash js-out]}]
  (let [out-dir  (io/file project-root ".boonmee")
        out-file (io/file out-dir (str hash ".ts"))]
    (io/make-parents out-dir)
    (io/make-parents out-file)
    (spit (str out-file) js-out)
    out-file))

(spit-src
 (project-root (io/file "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal"))
 {:js-out "foo"
  :hash "1234"
  }

 )
