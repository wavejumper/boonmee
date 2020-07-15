(ns dev
  (:require [boonmee.client.clojure :as boonmee]
            [clojure.core.async :as async]))

(defonce system
  (atom (boonmee/map->ClojureClient {:config (boonmee/config {})})))

(defn start! []
  (swap! system boonmee/start))

(defn stop! []
  (swap! system boonmee/stop))

(defn request!
  [msg]
  (async/put! (:req-ch @system) msg))

;; Scratchpad...

(comment
 (start!)
 #_(request! {:command   "open"
              :arguments {:file "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal/src/tonal/core.cljs"}})
 (request! {:command   "completions"
            :arguments {:file "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal/src/tonal/core.cljs"
                        :line  6
                        :offset 6}}))

(comment
 (:compiled
  (compiler/compile
   (io/file "examples/tonal/src/tonal/core.cljs")
   [(es6-import)
    (es6-symbol {:loc [4 3] :cursor? true})])))