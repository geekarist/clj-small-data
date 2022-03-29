(ns clj-small-data.core
  (:require [cljfx.api :as fx]))

(def initial-state-map
  {:title "Hello"
   :search-field-placeholder "Please enter your search text"
   :results
   [{:text "First result"}
    {:text "Second result"}
    {:text "Third result"}
    {:text "Fourth result"}
    {:text "Fifth result"}
    {:text "Sixth result"}
    {:text "Seventh result"}]})

(def state-atom
  (atom initial-state-map))

(defn root [{state-map :state}]
  {:fx/type :stage
   :showing true
   :title (state-map :title)
   :width 600
   :height 600
   :scene
   {:fx/type :scene
    :root
    {:fx/type :v-box
     :alignment :top-left
     :children
     (cons
      {:fx/type :text-field
       :v-box/margin {:left 8 :right 8}
       :text (state-map :search-field-placeholder)}
      (map (fn [result-map]
             {:fx/type :label
              :v-box/margin {:top 8 :left 8 :right 8}
              :text (result-map :text)})
           (state-map :results)))}}})

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc
                (fn [state-val]
                  {:fx/type root :state state-val}))))

(fx/mount-renderer state-atom renderer)

(comment
  (swap! state-atom (fn [_state-val] initial-state-map)))