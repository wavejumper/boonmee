(defproject boonmee "0.1.0"
  :description "cljs tooling"
  :url "https://github.com/wavejumper/boonmee"

  :dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                 [org.clojure/core.async "1.2.603"]
                 [me.raynes/conch "0.8.0"]
                 [cheshire "5.10.0"]
                 [rewrite-clj "0.6.1"]
                 [integrant "0.8.0"]]

  :profiles {:kaocha       {:dependencies [[lambdaisland/kaocha "1.0.632"]]}
             :native-image {:dependencies [[borkdude/clj-reflector-graal-java11-fix "0.0.1-graalvm-20.1.0"]
                                           [borkdude/sci.impl.reflector "0.0.1-java11"]]}
             :uberjar      {:global-vars {*assert* false}
                            :jvm-opts    ["-Dclojure.compiler.direct-linking=true"
                                          "-Dclojure.spec.skip-macros=true"]
                            :main        boonmee.client.stdio
                            :aot         :all}}

  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]}

  :repl-options {:init-ns boonmee.core})
