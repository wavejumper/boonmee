(ns boonmee.compiler.core
  (:refer-clojure :exclude [compile])
  (:require [clojure.string :as str]
            [boonmee.compiler.analyser :as ana]
            [boonmee.util :as util]
            [boonmee.logging :as log])
  (:import (java.io File)))

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
  (cond
    (nil? package-name)
    nil

    (empty? args)
    (str "import '" package-name "';")

    :else
    (str "import " (import-statements->str (import-statements args)) " from '" package-name "';")))

(defn compile-ns
  [es6-deps]
  (keep compile-es6-require es6-deps))

(defprotocol INode
  (-compile [this ctx]))

(defrecord Es6Import []
  INode
  (-compile [_ ctx]
    (compile-ns (:es6-deps ctx))))

(defn es6-import
  []
  (->Es6Import))

(defrecord Es6Sym
  [loc cursor?]
  INode
  (-compile [this ctx]
    (when-let [sym (ana/deduce-js-interop ctx loc)]
      [{:cursor? cursor?
        :js-out  (str (:sym sym) ".u")
        :sym     sym
        :sym-ctx this
        :ctx     ctx}])))

(defn es6-symbol
  [opts]
  (map->Es6Sym opts))

(defn compile-form
  [ctx form]
  (reduce
   (fn [state val]
     (cond
       (string? val)
       (-> state
           (update :js-out str (str val "\n"))
           (update :index inc))

       (and (map? val) (contains? val :cursor?))
       (cond
         (not (:cursor? val))
         (-> state
             (update :js-out str (str (:js-out val) "\n"))
             (update :index inc))

         (:line state)
         (throw (ex-info "Duplicate cursors declared"
                         {:state state :val val :form form :ctx ctx}))

         :else
         (let [next-idx (-> state :index inc)]
           (-> state
               (update :js-out str (str (:js-out val) "\n"))
               (assoc :index next-idx)
               (assoc :line next-idx)
               (assoc :offset (count (:js-out val))))))

       :else
       (throw (ex-info "Unhandled type compiling form"
                       {:state state :val val :form form :ctx ctx}))))
   {:index  0
    :js-out ""
    :line   nil
    :offset nil}
   (mapcat (fn [x] (-compile x ctx)) form)))

(defn file-name
  [ns ext js-out]
  (let [hash (subs (util/sha256 js-out) 0 6)]
    (-> (str ns "." ext)
        (str/replace #"\." "_")
        (str "_" hash ".ts"))))

(defn compile
  [^File file form]
  (let [ctx       (ana/analyse-file file)
        ext       (last (str/split (.getName file) #"\."))
        compiled  (compile-form ctx form)
        file-name (file-name (:ns ctx) ext (:js-out compiled))]
    (log/debugf "Compiling %s to %s" (:ns ctx) file-name)
    {:ctx       ctx
     :compiled  compiled
     :ext       ext
     :form      form
     :file-name file-name}))