(ns clj-small-data.core
  (:require [clj-small-data.runtime :as runtime]
            [clj-small-data.main :as main]))

(def app
  (runtime/create! main/init
                   (fn [] main/view) ; Makes it possible to reload the fn
                   runtime/upset))

(defn apply-changes! []
  (println "Applying changes")
  (runtime/apply-changes! app))

(apply-changes!)