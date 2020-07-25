(ns boonmee.logging
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [integrant.core :as ig])
  (:import (java.io Closeable)))

(defprotocol Logger
  (log [this msg]))

(defrecord StdoutLogger [ctx]
  Logger
  (log [_ msg]
    (println (merge ctx msg))))

(defn file-logger
  [fname]
  (let [ch       (async/chan)
        writer   (io/writer fname :append true)
        appender (async/go-loop []
                   (when-let [msg (async/<! ch)]
                     (.write writer (pr-str msg))
                     (.flush writer)
                     (recur)))]
    (reify
      Logger
      (log [_ m]
        (async/put! ch m))
      Closeable
      (close [_]
        (.close writer)
        (async/close! ch)
        (async/close! appender)))))

(defmethod ig/init-key :logger/file-logger
  [_ {:keys [fname]}]
  (file-logger fname))

(defmethod ig/halt-key! :logger/file-logger
  [_ ^Closeable logger]
  (.close logger))

(defmethod ig/init-key :logger/stdout-logger
  [_ {:keys [ctx]}]
  (StdoutLogger. ctx))

(defn debug
  ([logger msg]
   (log logger {:level :info :message msg}))
  ([logger e msg]
   (log logger {:level :info :error e :message msg})))

(defn info
  ([logger msg]
   (log logger {:level :info :message msg}))
  ([logger e msg]
   (log logger {:level :info :error e :message msg})))

(defn warn
  ([logger msg]
   (log logger {:level :warn :message msg}))
  ([logger e msg]
   (log logger {:level :warn :error e :message msg})))

(defn error
  ([logger msg]
   (log logger {:level :error :message msg}))
  ([logger e msg]
   (log logger {:level :error :error e :message msg})))

(defn logf
  [logger level [e? s & args]]
  (if (string? e?)
    (log logger {:level level :message (apply format e? s args)})
    (log logger {:level level :error e? :message (apply format s args)})))

(defn debugf
  [logger & args]
  (logf logger :debug args))

(defn infof
  [logger & args]
  (logf logger :info args))

(defn warnf
  [logger & args]
  (logf logger :warn args))

(defn errorf
  [logger & args]
  (logf logger :error args))