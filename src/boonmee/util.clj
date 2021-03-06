(ns boonmee.util
  (:require [clojure.java.io :as io])
  (:import (java.io Reader BufferedReader File Closeable IOException)
           (java.security MessageDigest)))

(defn -line-handler
  [^BufferedReader rdr ^Runnable f]
  (let [id     (str (gensym "line-handler*"))
        thread (Thread. f id)]
    (.start thread)
    (reify
      Closeable
      (close [_]
        (.close rdr)
        (.join thread)))))

(defn buffered-reader-open?
  [^BufferedReader reader]
  (try (.ready reader)
       true
       (catch IOException _ false)))

(defmacro line-handler
  [[binding ^Reader reader] & body]
  `(let [r# (BufferedReader. ~reader)
         f# (fn []
              (while (buffered-reader-open? r#)
                (when-let [~binding (.readLine r#)]
                  ~@body)))]
     (-line-handler r# f#)))

(defn sha256
  [^String string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))

(defn spit-src
  [^File project-root {:keys [file-name compiled]}]
  (if (and project-root (.exists project-root) (.isDirectory project-root))
    (let [out-dir  (io/file project-root ".boonmee")
          out-file (io/file out-dir file-name)]
      (io/make-parents out-dir)
      (io/make-parents out-file)
      (spit out-file (:js-out compiled))
      out-file)
    (throw (ex-info "project root does not exist or is not a directory"
                    {:project-root project-root}))))