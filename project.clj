(defproject boonmee "0.1.0"
  :description "cljs tooling"
  :url "https://github.com/wavejumper/boonmee"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.764"]
                 [clj-kondo "2020.06.21"]
                 [org.clojure/core.async "1.2.603"]
                 [me.raynes/conch "0.8.0"]
                 [cheshire "5.10.0"]
                 [rewrite-clj "0.6.1"]
                 [integrant "0.8.0"]
                 [org.clojure/tools.logging "1.1.0"]]

  :profiles {:kaocha {:dependencies [[lambdaisland/kaocha "1.0.632"]]}}

  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]}

  :repl-options {:init-ns boonmee.core})
