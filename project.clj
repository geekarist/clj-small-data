(defproject clj-small-data "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.cache "1.0.225"]
                 [cljfx "1.7.19"]
                 [org.clojure/data.json "2.4.1-SNAPSHOT"]
                 [cljs-ajax "0.8.4"]
                 [com.taoensso/timbre "5.2.1"]
                 [com.profesorfalken/jPowerShell "3.1.1"]]
  :repl-options {:init-ns clj-small-data.core})
