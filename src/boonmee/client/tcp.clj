(ns boonmee.client.tcp
  (:require [boonmee.client.stdio]
            [integrant.core :as ig])
  (:import (java.net Socket)
           (java.io InputStreamReader PrintWriter)))

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
   :boonmee/stdio-client                {:client-req-ch  (ig/ref :chan/client-req-ch)
                                         :client-resp-ch (ig/ref :chan/client-resp-ch)
                                         :in             (ig/ref :tcp/socket-reader)
                                         :out            (ig/ref :tcp/socket-writer)}
   :tcp/socket                          {:port 0
                                         :host "localhost"}
   :tcp/socket-reader                   {:socket (ig/ref :tcp/socket)}
   :tcp/socket-writer                   {:socket (ig/ref :tcp/socket)}
   :logger/stdout-logger                {}})

(defmethod ig/init-key
  :tcp/socket
  [_ {:keys [port host]}]
  (Socket. ^String host ^int port))

(defmethod ig/halt-key!
  :tcp/socket
  [_ socket]
  (.close ^Socket socket))

(defmethod ig/init-key
  :tcp/socket-reader
  [_ {:keys [^Socket socket]}]
  (InputStreamReader. (.getInputStream socket)))

(defmethod ig/halt-key!
  :tcp/socket-reader
  [_ reader]
  (.close ^InputStreamReader reader))

(defmethod ig/init-key
  :tcp/socket-writer
  [_ {:keys [^Socket socket]}]
  (PrintWriter. (.getOutputStream socket) true))

(defmethod ig/halt-key!
  :tcp/socket-writer
  [_ writer]
  (.close ^PrintWriter writer))