(ns boonmee.logging
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [integrant.core :as ig])
  (:import (java.io Writer)))

;; timbre or clojure.tools.logging make native images sad :(

(defprotocol Logger
  (log [this msg]))

(defrecord StdoutLogger []
  Logger
  (log [_ msg]
   (println msg)))

(defrecord AsyncLogger [ch]
  Logger
  (log [_ msg]
   (async/put! ch msg)))

(defonce logger
  (atom nil))

(defmethod ig/init-key :logger/file-logger
  [_ {:keys [fname]}]
  (let [ch (async/chan)
        w  (io/writer fname :append true)
        l  (AsyncLogger. ch)]
    (reset! logger l)
    {:ch      ch
     :writer  w
     :logger  l
     :go-loop (async/go-loop []
                (when-let [msg (async/<! ch)]
                  (.write w (pr-str msg))
                  (recur)))}))

(defmethod ig/halt-key! :logger/file-logger
  [_ {:keys [ch writer go-loop]}]
  (some-> ch async/close!)
  (some-> writer ^Writer .close)
  (some-> go-loop async/close!)
  (reset! logger nil))

(defmethod ig/init-key :logger/stdout-logger
  [_ _]
  (reset! logger (StdoutLogger.)))

(defmethod ig/halt-key! :logger/stdout-logger
  [_ _]
  (reset! logger nil))

(defn log*
  [m]
  (when-let [logger @logger]
    (log logger m)))

(defn debug
  ([msg]
   (log* {:level :info :message msg}))
  ([e msg]
   (log* {:level :info :error e :message msg})))

(defn info
  ([msg]
   (log* {:level :info :message msg}))
  ([e msg]
   (log* {:level :info :error e :message msg})))

(defn warn
  ([msg]
   {:level :warn :message msg})
  ([e msg]
   {:level :warn :error e :message msg}))

(defn error
  ([msg]
   (log* {:level :error :message msg}))
  ([e msg]
   (log* {:level :error :error e :message msg})))

(defn logf*
  [level [e? s & args]]
  (if (string? e?)
    (log* {:level level :message (apply format e? s args)})
    (log* {:level level :error e? :message (apply format s args)})))

(defn debugf
  [& args]
  (logf* :debug args))

(defn infof
  [& args]
  (logf* :info args))

(defn warnf
  [& args]
  (logf* :warn args))

(defn errorf
  [& args]
  (logf* :error args))