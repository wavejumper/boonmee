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
  [env zip]
  (let [{:keys [ns deps]} (npm-deps zip)]
    {:npm-deps deps
     :npm-syms (into #{'js} (npm-syms deps))
     :ns       ns
     :zip      zip
     :env      env}))

(defn analyse-file
  [env ^File f]
  (analyse env (z/of-file f {:track-position? true})))

(defn analyse-string
  [env s]
  (analyse env (z/of-string s {:track-position? true})))

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

(defn fragment->sym
  [fragment]
  (when-let [syms (some-> fragment
                          str
                          (str/replace-first #"\.-" "")
                          (str/split #"\."))]
    (into [] (comp (remove str/blank?)
                   (map symbol))
          syms)))

(defn interpret-arg
  [ctx arg]
  (cond
    (symbol? arg)
    (let [sym      (symbol (or (namespace arg) (name arg)))
          interop? ((:npm-syms ctx) sym)]
      {:type     :symbol
       :interop? interop?
       :sym      sym
       :value    arg
       :name     (name arg)
       :ns       (namespace arg)})

    (string? arg)
    {:type  :string
     :value arg}

    (keyword? arg)
    {:type  :keyword
     :value arg}))

(defn interpret-list
  [ctx parent]
  (let [first-arg        (-> parent z/next z/sexpr)
        second-arg       (-> parent z/next z/next z/sexpr)
        third-arg        (-> parent z/next z/next z/next z/sexpr)
        first-arg-intrp  (interpret-arg ctx first-arg)
        second-arg-intrp (interpret-arg ctx second-arg)
        third-arg-intrp  (interpret-arg ctx third-arg)]
    (cond
      (:interop? first-arg-intrp)
      {:fragments (when (:ns first-arg-intrp)
                    (fragment->sym (:name first-arg-intrp)))
       :sym       (:sym first-arg-intrp)
       :global?   (= 'js (:sym first-arg-intrp))
       :usage     (if (str/ends-with? (:name first-arg-intrp) ".")
                    :constructor
                    :method)}

      (and (= :symbol (:type first-arg-intrp)) (:interop? second-arg-intrp))
      (let [aset?            (= 'aset (:value first-arg-intrp))
            aget?            (= 'aget (:value first-arg-intrp))
            property-use?    (or (str/starts-with? (:name first-arg-intrp) ".-")
                                 aget? aset?)
            method-use?      (and (not property-use?)
                                  (str/starts-with? (:name first-arg-intrp) "."))

            second-ns        (:ns second-arg-intrp)
            second-fragments (when second-ns
                               (fragment->sym (:name second-arg-intrp)))
            sym              (:sym second-arg-intrp)]
        {:fragments (cond
                      (or aset? aget?)
                      (vec (distinct (into second-fragments (fragment->sym (:value third-arg-intrp)))))

                      (or property-use? method-use?)
                      (vec (distinct (into second-fragments (fragment->sym (:value first-arg-intrp)))))

                      :else
                      (fragment->sym (:name second-arg-intrp)))
         :sym       sym
         :global?   (= 'js sym)
         :usage     (cond
                      property-use? :property
                      method-use? :method
                      :else :unknown)}))))

(defn interpret-vector
  [ctx parent]
  (let [first-arg        (-> parent z/next z/sexpr)
        second-arg       (-> parent z/next z/next z/sexpr)
        first-arg-intrp  (interpret-arg ctx first-arg)
        second-arg-intrp (interpret-arg ctx second-arg)]
    (when (and (= :string (:type first-arg-intrp))
               (or (nil? second-arg)
                   (#{:as :refer :default} (:value second-arg-intrp)))
               ((:npm-syms ctx) (symbol (:value first-arg-intrp))))
      {:fragments nil
       :sym       (symbol (:value first-arg-intrp))
       :global?   false
       :usage     :require})))

(defn interpret-symbol
  [ctx arg]
  (let [ns       (namespace arg)
        name     (name arg)
        sym      (symbol (or ns name))
        interop? ((:npm-syms ctx) sym)]
    (when interop?
      {:fragments (when ns
                    (fragment->sym name))
       :sym       sym
       :global?   (= 'js sym)
       :usage     :property})))

(extend-protocol IInterop
  Symbol
  (interop [this ctx zip]
    (let [parent       (z/up zip)
          parent-sexpr (z/sexpr parent)]
      (cond
        ;; fn invocation...
        (list? parent-sexpr)
        (or (interpret-list ctx parent)
            ;; top-level symbol
            (interpret-symbol ctx this))

        ;; for require forms
        (vector? parent-sexpr)
        (interpret-vector ctx parent))))

  String
  (interop [this ctx zip]
    (let [parent        (z/sexpr (z/up zip))
          require-form? (and (vector? parent)
                             (s/valid? :clojure.core.specs.alpha/ns-require
                                       ;; I don't want to bring in cljs as a dep :'(
                                       [:require (update parent 0 #(when (string? %) (symbol %)))])
                             (-> parent first string?))
          prev-zip      (z/prev zip)
          prev-sexpr    (z/sexpr prev-zip)]
      (cond
        require-form?
        {:fragments nil
         :sym       this
         :global?   false
         :usage     :require}

        ;; for aget/aset etc
        (symbol? prev-sexpr)
        (interop prev-sexpr ctx prev-zip))))

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