(ns boonmee.client.dev
  (:require [boonmee.server]
            [boonmee.tsserver.server]
            [integrant.core :as ig]
            [clojure.core.async :as async])
  (:gen-class))

(defmethod ig/init-key
  :boonmee/dev-client
  [_ {:keys [client-req-ch client-resp-ch]}]
  {:out (async/go-loop []
          (when-let [resp (async/<! client-resp-ch)]
            (println resp)
            (recur)))
   :in  client-req-ch})

(defmethod ig/halt-key!
  :boonmee/dev-client
  [_ {:keys [out]}]
  (some-> out async/close!))

(defn config []
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
   :boonmee/dev-client                  {:client-req-ch  (ig/ref :chan/client-req-ch)
                                         :client-resp-ch (ig/ref :chan/client-resp-ch)}})

(defonce system
  (atom nil))

(defn start!
  []
  (reset! system (ig/init (config))))

(defn stop!
  []
  (some-> system deref ig/halt!)
  (reset! system nil))

(defn request!
  [req]
  (when-let [in (some-> system deref :boonmee/client :in)]
    (async/put! in req)))

(defn -main
  [& _]
  (start!))

(comment
 (start!)
 #_(request! {:command   "open"
              :arguments {:file "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal/src/tonal/core.cljs"}})
 (request! {:command   "completions"
            :arguments {:file "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal/src/tonal/core.cljs"
                        :line  6
                        :offset 6}}))