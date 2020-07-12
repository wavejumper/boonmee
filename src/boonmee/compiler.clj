(ns boonmee.compiler
  (:refer-clojure :exclude [compile])
  (:require [clojure.string :as str]
            [boonmee.analyzer :as ana]
            [boonmee.util :as util]))

(defn import-statements
  [args]
  (reduce
   (fn [state [type vs]]
     (case type
       :as (assoc state :as vs)
       :default (assoc state :default vs)
       :rename (update state :refer into vs)
       :refer (update state :refer into vs)))
   {}
   args))

(defn refer->str
  [refer]
  (let [refer-args (map (fn [x]
                          (if (symbol? x)
                            (str x)
                            (str (first x) " as " (second x))))
                        refer)]
    (str "{ " (str/join ", " refer-args) " }")))

(defn import-statements->str
  [{:keys [as default refer]}]
  (str/join ", "
            (cond-> []
              default (conj default)
              as (conj (str "* as " as))
              refer (conj (refer->str refer)))))

(defn compile-es6-require
  [{:keys [package-name args]}]
  (if (empty? args)
    (str "import " package-name ";")
    (str "import " (import-statements->str (import-statements args)) " from \"" package-name "\";")))

(defn compile-ns
  [es6-deps]
  (str/join "\n" (map compile-es6-require es6-deps)))

(defn compile
  [file]
  (let [src (slurp file)
        ctx (ana/analyze src)]
    {:ctx    ctx
     :js-out (compile-ns (:es6-deps ctx))
     :hash   (util/sha256 src)}))