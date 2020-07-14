(ns boonmee.util
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async])
  (:import (java.io BufferedReader File Closeable)
           (java.security MessageDigest)))

(defn line-handler*
  [rdr ch]
  (reify
    Closeable
    (close [_]
      (.close rdr)
      (async/close! ch))))

(defmacro line-handler
  [[line-binding reader] & body]
  `(let [r#  (BufferedReader. ~reader)
         ch# (async/thread
              (doseq [~line-binding (line-seq r#)]
                ~@body))]
     (line-handler* r# ch#)))

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
  [^File project-root {:keys [file-name compiled]}]
  (when (and project-root (.exists project-root))
    (let [out-dir  (io/file project-root ".boonmee")
          out-file (io/file out-dir file-name)]
      (io/make-parents out-dir)
      (io/make-parents out-file)
      (spit (str out-file) (:js-out compiled))
      out-file)))