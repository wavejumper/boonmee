(ns boonmee.compiler.analyser
  (:require [rewrite-clj.zip :as z]
            [clojure.string :as str]
            [clojure.core.specs.alpha]
            [clojure.spec.alpha :as s])
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
  [zip]
  (let [{:keys [ns deps]} (es6-deps zip)]
    {:es6-deps deps
     :es6-syms (set (es6-syms deps))
     :ns       ns
     :zip      zip}))

(defn analyse-file
  [^File f]
  (analyse (z/of-file f {:track-position? true})))

(defn analyse-string
  [s]
  (analyse (z/of-string s {:track-position? true})))

(defn location
  [{:keys [zip]} [line offset]]
  (loop [zip zip]
    (let [next-zip (z/next zip)
          [curr-line curr-offset] (z/position next-zip)
          on-line? (= curr-line line)]
      (cond
        (z/end? next-zip)
        next-zip

        (< curr-line line)
        (recur next-zip)

        (and on-line? (>= curr-offset offset))
        zip

        (and on-line? (> (-> next-zip z/next z/position first) line))
        next-zip

        on-line?
        (recur next-zip)

        :else
        next-zip))))

(defprotocol IInterop
  (interop [this ctx zip]))

(extend-protocol IInterop
  Symbol
  (interop [this ctx zip]
    (cond
      (namespace this)
      (when-let [sym ((:es6-syms ctx) (-> this namespace symbol))]
        {:fragment (-> this name symbol)
         :sym      sym
         :usage    :method})

      (contains? (:es6-syms ctx) this)
      (let [[fragment usage]
            (let [prev-sexpr (-> zip z/prev z/sexpr)]
              (when (symbol? prev-sexpr)
                (if (#{'aget 'aset} prev-sexpr)
                  [(-> zip z/next z/sexpr) (if (= 'aget prev-sexpr) :property :method)]
                  [prev-sexpr :method])))]
        {:fragment fragment
         :sym      this
         :usage    (or usage :unknown)})

      (str/starts-with? (str this) ".")
      (let [next-zip   (z/next zip)
            next-sexpr (z/sexpr next-zip)]
        (when-let [sym (interop next-sexpr ctx next-zip)]
          {:fragment this
           :sym      sym
           :usage    (if (str/starts-with? (str this) ".-")
                       :property
                       :method)}))))

  String
  (interop [this _ zip]
    (let [parent (z/sexpr (z/up zip))]
      (if (and (vector? parent)
               (s/valid? :clojure.core.specs.alpha/ns-require
                         ;; I don't want to bring in cljs as a dep :'(
                         [:require (update parent 0 #(when (string? %) (symbol %)))])
               (-> parent first string?))
        {:fragment nil
         :sym      this
         :usage    :require})))

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
    (some-> zip z/sexpr (interop ctx zip))))