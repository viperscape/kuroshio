(defproject kuroshio "0.2.0-SNAPSHOT"
  :description "stream-like thread communication"
  :url "https://github.com/viperscape/kuroshio"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot kuroshio.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
