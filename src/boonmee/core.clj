(ns boonmee.core
  (:require [boonmee.tsserver]
            [boonmee.server]
            [boonmee.client]
            [clojure.core.async :as async]
            [integrant.core :as ig]))

(defmethod ig/init-key :async/chan
  [_ _]
  (async/chan))

(defmethod ig/halt-key! :async/chan
  [_ ch]
  (some-> ch async/close!))

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
   :boonmee/client                      {:client-req-ch  (ig/ref :chan/client-req-ch)
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

(comment
 (start!)
 (request! {:command   "open"
            :arguments {:file "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal/src/tonal/core.cljs"}})
 (request! {:command   "completions"
            :arguments {:file "/Users/thomascrowley/Code/clojure/boonmee/examples/tonal/src/tonal/core.cljs"}})
 )