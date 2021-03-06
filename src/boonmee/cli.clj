(ns boonmee.cli
  (:require [boonmee.client.stdio :as client.stdio]
            [clojure.java.io :as io]
            [clojure.tools.cli :as tools.cli]
            [integrant.core :as ig])
  (:import (java.util.concurrent CountDownLatch))
  (:gen-class))

(def cli-options
  [["-c" "--client client" "Specify client"
    :default "stdio"
    :validate [#{"stdio"} "Must be either #{stdio"]]

   ["-e" "--env env" "JS Environment"
    :default "browser"
    :validate [#{"browser" "node"} "Must be either #{browser node}"]]

   ["-T" "--tsserver tsserver" "tsserver"
    :default "tsserver"]

   ["-L" "--log-file log-file" "Log file location"
    :default "boonmee.log"]

   ["-v" "--version"]

   ["-h" "--help"]])

(defn -main
  [& args]
  (let [opts  (tools.cli/parse-opts args cli-options)
        latch (CountDownLatch. 1)]

    (when-let [errors (seq (:errors opts))]
      (doseq [error errors]
        (println error))
      (System/exit 1))

    (when (-> opts :options :help)
      (println (:summary opts))
      (System/exit 0))

    (when (-> opts :options :version)
      (println (slurp (io/resource "version")))
      (System/exit 0))

    (let [config (client.stdio/config (:options opts))]
      (try
        (let [system (ig/init config)]
          (.addShutdownHook
           (Runtime/getRuntime)
           (Thread. ^Runnable (fn []
                                (try
                                  (ig/halt! system)
                                  (catch Throwable e
                                    (.printStackTrace e)))
                                (.countDown latch)))))
        (.await latch)
        (System/exit 0)
        (catch Throwable e
          (.printStackTrace e)
          (System/exit 1))))))
