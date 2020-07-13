(ns boonmee.analyser
  (:require [rewrite-clj.zip :as z]
            [clojure.string :as str])
  (:import (clojure.lang Symbol PersistentList)
           (java.io File)))

(defn analyse-es6-require
  [[package-name & args]]
  {:package-name package-name
   :args         (apply hash-map args)})

(defn es6-deps
  [data]
  (when-let [ns (-> (z/find-value data z/next 'ns)
               z/up
               z/sexpr)]
    {:ns   (second ns)
     :deps (->> ns
                (filter list?)
                (filter (comp #{:require} first))
                (first)
                (rest)
                (filter (comp string? first))
                (map analyse-es6-require))}))

(defn es6-syms
  [deps]
  (mapcat (fn [{:keys [args]}]
            (cond-> #{}
              (:refer args) (into (map (fn [x]
                                         (if (symbol? x)
                                           x
                                           (second x)))
                                       (:refer args)))
              (:as args) (conj (:as args))
              (:default args) (conj (:default args))))
          deps))

(defn analyse
  [^File f]
  (let [zip  (z/of-file f {:track-position? true})
        {:keys [ns deps]} (es6-deps zip)]
    {:es6-deps deps
     :es6-syms (set (es6-syms deps))
     :ns       ns
     :zip      zip}))

(defn location
  [{:keys [zip]} [x y]]
  (loop [zip zip]
    (let [next-zip (z/next zip)
          [curr-x curr-y] (z/position next-zip)]
      (cond
        (z/end? next-zip)
        next-zip

        (< curr-x x)
        (recur next-zip)

        (= curr-x x)
        (cond
          (>= curr-y y)
          zip

          (> (-> next-zip z/next z/position second) x)
          next-zip

          :else
          (recur next-zip))))))

(defprotocol IInterop
  (interop [this ctx zip]))

(extend-protocol IInterop
  Symbol
  (interop [this ctx zip]
    (cond
      (namespace this)
      {:fragment (-> this name symbol)
       :sym      ((:es6-syms ctx) (-> this namespace symbol))}

      (contains? (:es6-syms ctx) this)
      {:fragment nil
       :sym      this}

      (str/starts-with? (str this) ".")
      (let [next-zip (z/next zip)
            next-sexpr (z/sexpr next-zip)]
        {:fragment this
         :sym      (interop next-sexpr ctx next-zip)})

      (= 'aget this)
      (do (println "TODO: implement aget")
          nil)

      (= 'aset this)
      (do (println "TODO: implement aset")
          nil)))

  PersistentList
  (interop [_ ctx zip]
    (let [next-zip   (z/next zip)
          next-sexpr (z/sexpr next-zip)]
      (interop next-sexpr ctx next-zip)))

  Object
  (interop [_ _ _]
   nil))

(defn deduce-js-interop
  [ctx loc]
  (when-let [zip (location ctx loc)]
    (some-> zip z/sexpr (interop  ctx zip))))