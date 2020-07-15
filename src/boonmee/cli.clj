(ns boonmee.cli
  (:require [boonmee.client.stdio :as client.stdio]
            [clojure.tools.cli :as tools.cli]
            [integrant.core :as ig])
  (:import (java.util.concurrent CountDownLatch))
  (:gen-class))

(def cli-options
  ;; An option with a required argument
  [["-c" "--client client" "Specify client"
    :default "stdio"
    :validate [#{"stdio" "tcp"} "Must be either #{stdio tcp}"]]])

(defn -main
  [& args]
  (let [opts  (tools.cli/parse-opts args cli-options)
        latch (CountDownLatch. 1)]
    (when-let [errors (seq (:errors opts))]
      (doseq [error errors]
        (println error))
      (System/exit 1))

    (let [config (case (-> :options :client)
                   "stdio" (client.stdio/config opts)
                   "tcp" {})]
      (try
        (let [system (ig/init config)]
          (.addShutdownHook
           (Runtime/getRuntime)
           (Thread. ^Runnable (fn []
                                (try
                                  (ig/halt-key! system)
                                  (catch Throwable e
                                    (.printStackTrace e)))
                                (.countDown latch)))))
        (.await latch)
        (catch Throwable e
          (.printStackTrace e)
          (System/exit 1)))

      (System/exit 0))))