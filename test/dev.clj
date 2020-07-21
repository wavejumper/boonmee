(ns dev
  (:require [boonmee.client.clojure :as boonmee]
            [clojure.core.async :as async]))

(def config
  {:env      "node"
   :tsserver "tsserver"})

(defonce system
  (atom (boonmee/map->ClojureClient {:config (boonmee/config config)})))

(defn start! []
  (swap! system boonmee/start))

(defn stop! []
  (swap! system boonmee/stop))

(defn request!
  [msg]
  (when-let [req-ch (:req-ch @system)]
    (async/put! req-ch msg)))

(defn response!
  [timeout-ms]
  (when-let [resp-ch (:resp-ch @system)]
    (let [timeout-ch (async/timeout timeout-ms)
          [val ch] (async/alts!! [resp-ch timeout-ch])]
      (when-not (= ch timeout-ch)
        val))))