(defproject enlive "1.2.0-SNAPSHOT"
  :min-lein-version "2.0.0"
  :description "a HTML selector-based (à la CSS) templating and transformation system for Clojure"
  :url "http://github.com/cgrand/enlive/"
  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.ccil.cowan.tagsoup/tagsoup "1.2.1"]
                 [org.jsoup/jsoup "1.7.2"]]
  :profiles {:dev {:resource-paths ["test/resources"]}
             :test {:plugins [[lein-cljsbuild "1.0.6"]
                              [lein-shell "0.4.0"]]
                    :dependencies [[org.clojure/clojurescript "0.0-3308"]]
                    :resource-paths ["test/resources"]
                    :aliases {"test-clj" ["run" "-m" "net.cgrand.enlive-html.test"]
                              "test-cljs" ["do" ["clean"]
                                                ["cljsbuild" "once" "whitespace"]
                                                ["shell" "phantomjs" "target/js/test-ws.js"]]}
                    :cljsbuild {:builds {:whitespace {:source-paths ["src" "test"]
                                                      :compiler {:output-to "target/js/test-ws.js"
                                                                 :optimizations :whitespace}}
                                         :advanced {:source-paths ["src" "test"]
                                                    :compiler {:output-to "target/js/test-advanced.js"
                                                               :optimizations :advanced}}}}}})
