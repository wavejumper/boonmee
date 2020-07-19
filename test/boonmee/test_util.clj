(ns boonmee.test-util
  (:require [boonmee.client.clojure :as client]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [rewrite-clj.zip :as z]))

(defn clean-up-boonmee
  [_]
  (println "Clean up!"))

(defn client-config []
  (if-let [f (io/resource "tonal/node_modules/typescript/bin/tsserver")]
    (client/config {:tsserver/proc (.getFile f)})
    (throw (RuntimeException. "tsserver not found on classpath."))))

(defmacro with-client [[sym opts] & body]
  `(let [opts#   ~opts
         client# (client/start (client/map->ClojureClient {:config (client-config)}))]
     (try
       (let [~sym client#]
         ~@body)
       (finally
         (client/stop client#)
         (clean-up-boonmee opts#)))))

(defn response!
  [client timeout-ms]
  (let [timeout-ch (async/timeout timeout-ms)
        [val ch] (async/alts!! [(:resp-ch client) timeout-ch])]
    (when-not (= ch timeout-ch)
      val)))

(defn request!
  [client msg]
  (async/put! (:req-ch client) msg))


(defn locations
  [form]
  (let [zip (z/of-string (pr-str form) {:track-position? true})]
    (loop [locs [[(z/sexpr zip) (z/position zip)]]
           zip  zip]
      (let [next-zip (z/next zip)
            pos      (z/position next-zip)]
        (if
         (z/end? next-zip)
          locs

          (recur (conj locs [(z/sexpr next-zip) pos])
                 next-zip))))))