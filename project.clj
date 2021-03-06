(def version (slurp "resources/version"))

(defproject wavejumper/boonmee `~version
  :description "Clojure language server"
  :url "https://github.com/wavejumper/boonmee"

  :dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                 [org.clojure/core.async "1.2.603"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/tools.cli "1.0.194"]
                 [wavejumper/conch "0.9.3"]
                 [rewrite-clj "0.6.1"]
                 [integrant "0.8.0"]
                 [expound "0.8.5"]]

  :resource-paths ["resources"]

  :profiles {:dev          {:test-paths     ["test"]
                            :resource-paths ["dev-resources"]
                            :repl-options   {:init-ns dev}
                            :main           boonmee.cli}
             :kaocha       {:dependencies [[lambdaisland/kaocha "1.0.632"]]}
             :native-image {:dependencies [[borkdude/clj-reflector-graal-java11-fix "0.0.1-graalvm-20.1.0"]]}
             :uberjar      {:global-vars {*assert* false}
                            :jvm-opts    ["-Dclojure.compiler.direct-linking=true"
                                          "-Dclojure.spec.skip-macros=true"]
                            :main        boonmee.cli
                            :aot         :all}}

  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]})
