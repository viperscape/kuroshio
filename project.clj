(defproject kuroshio "0.2.3-SNAPSHOT"
  :description "creating and operating on streams (delayed, lazily-evaluated, endless lists), enables simple thread communication via stream and chan"
  :url "https://github.com/viperscape/kuroshio"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot kuroshio.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
