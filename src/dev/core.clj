(ns dev.core
  (:require [clj-small-data.core :as sd]))

(defn apply-changes! []
  (sd/apply-changes!))

(comment
  (apply-changes!))