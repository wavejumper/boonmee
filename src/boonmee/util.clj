(ns boonmee.util
  (:import (java.io File)
           (java.security MessageDigest)))

(defn sha256
  [string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))

(defn project-root
  [^File file])