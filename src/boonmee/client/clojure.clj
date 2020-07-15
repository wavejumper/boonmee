(ns boonmee.client.clojure
  (:require [boonmee.server]
            [boonmee.tsserver.server]
            [boonmee.logging]
            [integrant.core :as ig])
  (:gen-class))

(defn config
  [_]
  {[:async/chan :chan/tsserver-resp-ch] {}
   [:async/chan :chan/tsserver-req-ch]  {}
   [:async/chan :chan/client-resp-ch]   {}
   [:async/chan :chan/client-req-ch]    {}
   :boonmee/tsserver                    {:tsserver-resp-ch (ig/ref :chan/tsserver-resp-ch)
                                         :tsserver-req-ch  (ig/ref :chan/tsserver-req-ch)}
   :boonmee/server                      {:tsserver-resp-ch (ig/ref :chan/tsserver-resp-ch)
                                         :tsserver-req-ch  (ig/ref :chan/tsserver-req-ch)
                                         :client-req-ch    (ig/ref :chan/client-req-ch)
                                         :client-resp-ch   (ig/ref :chan/client-resp-ch)
                                         :ctx              {}}
   :logger/stdout-logger                {}})

(defprotocol IClojureClient
  (start [this])
  (stop [this]))

(defrecord ClojureClient [config]
  IClojureClient
  (start [this]
    (let [sys     (ig/init config)
          req-ch  (get sys [:async/chan :chan/client-resp-ch])
          resp-ch (get sys [:async/chan :chan/client-resp-ch])]
      (assoc this :system sys :req-ch req-ch :resp-ch resp-ch)))
  (stop [this]
    (some-> this :sys ig/halt!)
    (dissoc this :system :req-ch :resp-ch)))

(defn client
  [opts]
  (start (ClojureClient. (config opts))))