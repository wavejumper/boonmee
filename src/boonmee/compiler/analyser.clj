(ns boonmee.compiler.analyser
  (:require [rewrite-clj.zip :as z]
            [clojure.string :as str]
            [clojure.core.specs.alpha]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang Symbol PersistentList)
           (java.io File)))

(defn analyse-npm-require
  [[package-name & args]]
  {:package-name package-name
   :args         (apply hash-map args)})

(defn npm-deps
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
                (map analyse-npm-require))}))

(defn npm-syms
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
  (let [{:keys [ns deps]} (npm-deps zip)]
    {:npm-deps deps
     :npm-syms (set (npm-syms deps))
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
      (when-let [sym ((:npm-syms ctx) (-> this namespace symbol))]
        {:fragment (-> this name symbol)
         :global?  (= 'js sym)
         :sym      sym
         :usage    :method})

      (contains? (:npm-syms ctx) this)
      (let [[fragment usage]
            (let [prev-sexpr (-> zip z/prev z/sexpr)]
              (when (symbol? prev-sexpr)
                (if (#{'aget 'aset} prev-sexpr)
                  [(-> zip z/next z/sexpr) (if (= 'aget prev-sexpr) :property :method)]
                  [prev-sexpr :method])))]
        {:fragment fragment
         :global?  false
         :sym      this
         :usage    (or usage :unknown)})

      (str/starts-with? (str this) ".")
      (let [next-zip   (z/next zip)
            next-sexpr (z/sexpr next-zip)]
        (when-let [next-sym (interop next-sexpr ctx next-zip)]
          {:fragment this
           :sym      (:sym next-sym)
           :global?  (:global? next-sym)
           :usage    (if (str/starts-with? (str this) ".-")
                       :property
                       :method)}))))

  String
  (interop [this ctx zip]
    (let [parent        (z/sexpr (z/up zip))
          require-form? (and (vector? parent)
                             (s/valid? :clojure.core.specs.alpha/ns-require
                                       ;; I don't want to bring in cljs as a dep :'(
                                       [:require (update parent 0 #(when (string? %) (symbol %)))])
                             (-> parent first string?))
          fn-inv        (some-> zip z/prev z/prev z/sexpr)
          aset-call?    (= 'aset fn-inv)
          aget-call?    (= 'aget fn-inv)
          prev-sexpr    (z/sexpr (z/prev zip))
          sym           (when (symbol? prev-sexpr)
                          (if (namespace prev-sexpr)
                            ((:npm-syms ctx) (-> prev-sexpr namespace symbol))
                            ((:npm-syms ctx) prev-sexpr)))
          global?      (and (symbol? prev-sexpr) (= "js" (namespace prev-sexpr)))]
      (cond
        (and (or aset-call? aget-call?) sym)
        {:fragment this
         :sym      sym
         :global?  global?
         :usage    (if aget-call?
                     :property
                     :method)}

        require-form?
        {:fragment nil
         :sym      this
         :global?  global?
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
    (some-> zip
            z/sexpr
            (interop ctx zip)
            (assoc :prev-location (-> zip z/prev z/position))
            (assoc :next-location (-> zip z/next z/position)))))