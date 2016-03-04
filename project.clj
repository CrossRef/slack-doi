(defproject crossref/slack-doi "0.1.0-SNAPSHOT"
  :description "Look up DOIs"
  :url "http://github.com/CrossRef/slack-doi"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.18"]
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [environ "1.0.2"]
                 [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot slack-doi.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
