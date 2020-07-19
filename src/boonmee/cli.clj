(ns boonmee.cli
  (:require [boonmee.client.stdio :as client.stdio]
            [boonmee.client.tcp :as client.tcp]
            [clojure.tools.cli :as tools.cli]
            [integrant.core :as ig])
  (:import (java.util.concurrent CountDownLatch))
  (:gen-class))

(def cli-options
  [["-c" "--client" "Specify client"
    :default "stdio"
    :validate [#{"stdio" "tcp"} "Must be either #{stdio tcp}"]]

   ["-p" "--port" "Port number"
    :default 9457
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

   ["-e" "--env" "JS Environment"
    :default "browser"
    :validate [#{"browser" "node"} "Must be either #{browser node}"]]

   ["-H" "--heartbeat" "TCP heartbeat (ms)"
    :default 30000
    :parse-fn #(Integer/parseInt %)]

   ["-T" "--tsserver" "tsserver"
    :default "tsserver"]

   ["-Tp" "--tsserver-port" "tsserver port"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

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

    (let [config (case (-> opts :options :client)
                   "stdio" (client.stdio/config (:options opts))
                   "tcp"   (client.tcp/config (:options opts)))]
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