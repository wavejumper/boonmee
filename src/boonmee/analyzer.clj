(ns boonmee.analyzer
  (:require [clojure.string :as str]
            [rewrite-clj.parser :as p]
            [rewrite-clj.zip :as z]))

(defn ->import-statement
  [args]
  (reduce
   (fn [state [type vs]]
     (case type
       :as      (assoc state :as vs)
       :default (assoc state :default vs)
       :rename  (update state :refer into vs)
       :refer   (update state :refer into vs)))
   {}
   args))

(defn refer->str
  [refer]
  (let [refer-args (map (fn [x]
                          (if (symbol? x)
                            (str x)
                            (str (first x) " as " (second x))))
                        refer)]
    (str "{ " (str/join  ", " refer-args) " }")))

(defn import-statements->str
  [{:keys [as default refer]}]
  (str/join ", "
            (cond-> []
              default (conj default)
              as (conj (str "* as " as))
              refer (conj (refer->str refer)))))

(defn require->import
  [{:keys [package-name args]}]
  (if (empty? args)
    (str "import " package-name ";")
    (let [import-statements (->import-statement args)]
      (str "import " (import-statements->str import-statements) " from " package-name ";"))))

(defn analyze-es6-require
  [[package-name & args]]
  (let [ctx {:package-name package-name
             :args         (apply hash-map args)}]
    (assoc ctx :js (require->import ctx))))

(analyze-npm-require
 '["module-name" :refer (export1) :rename {export2 alias2}])

(defn file->es6-deps
  [file]
  (->> (z/of-file file)
       (z/sexpr)
       (filter list?)
       (filter (comp #{:require} first))
       (first)
       (rest)
       (filter (comp string? first))
       (map analyze-es6-require)))