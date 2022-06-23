(ns dev.core
  (:require [clj-small-data.core :as sd]
            [clojure.java.browse :refer [browse-url]]
            [clojure.string :as str]))

(defn apply-changes! []
  (sd/apply-changes!))

(defn ggl! [query]
  (browse-url
   (str "https://google.fr/search?q=" (str/replace query " " "%20"))))

(comment
  (apply-changes!))