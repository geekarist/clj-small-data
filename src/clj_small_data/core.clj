(ns clj-small-data.core
  (:require [clj-small-data.runtime :as runtime]
            [clj-small-data.finder :as finder]))

(def app
  (runtime/create! finder/init
                   (fn [] finder/view) ; Makes it possible to reload the fn
                   finder/upset))

(defn apply-changes! []
  (println "Applying changes")
  (runtime/apply-changes! app))

(apply-changes!)