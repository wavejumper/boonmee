(ns boonmee.test-util
  (:require [boonmee.client.clojure :as client]
            [clojure.core.async :as async]
            [rewrite-clj.zip :as z]))

(defn clean-up-boonmee
  [client]
  (let [req-ch (:req-ch client)]
    (async/>!! req-ch {:command   "flush"
                       :type      "request"
                       :requestId (str (gensym "flush"))})

    (client/stop client)))

(def default-opts
  {:tsserver "tsserver"})

(defn client-config
  [opts]
  (client/config (merge default-opts opts)))

(defmacro with-client [[sym opts] & body]
  `(let [opts#   ~opts
         client# (client/start (client/map->ClojureClient {:config (client-config opts#)}))]
     (try
       (let [~sym client#]
         ~@body)
       (finally
         (clean-up-boonmee client#)))))

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