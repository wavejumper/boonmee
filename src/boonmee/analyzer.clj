(ns boonmee.analyzer
  (:require [rewrite-clj.zip :as z]))

(defn analyze-es6-require
  [[package-name & args]]
  {:package-name package-name
   :args         (apply hash-map args)})

(defn es6-deps
  [zip]
  (->> zip
       (z/sexpr)
       (filter list?)
       (filter (comp #{:require} first))
       (first)
       (rest)
       (filter (comp string? first))
       (map analyze-es6-require)))

(defn analyze
  [s]
  (let [zip (z/of-string s)]
    {:es6-deps (es6-deps zip)}))