(ns clj-small-data.core
  (:require [cljfx.api :as fx]))

(def initial-state-map
  {:title "Hello again!"
   :search-field-placeholder "Please enter your search text"
   :results
   [{:text "1st result"}
    {:text "2nd result"}
    {:text "3rd result"}
    {:text "4th result"}
    {:text "5th result"}
    {:text "6th result"}
    {:text "7th result"}]})

(def state-atom
  (atom initial-state-map))

(defn reload-state []
  (swap!
   state-atom
   (fn [_state-val]
     initial-state-map)))

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
     :padding 16
     :style {:-fx-background-color "#ffff00"}
     :children
     (cons
      {:fx/type :h-box
       :style {:-fx-background-color "#00ff00"}
       :children
       [{:fx/type :text-field
         :style {:-fx-background-color "#ff0000"}
         :h-box/hgrow :always
         :h-box/margin {:right 8}
         :prompt-text (state-map :search-field-placeholder)}
        {:fx/type :button :text "Search"
         :on-action (fn [_] (reload-state))}]}
      (map (fn [result-map]
             {:fx/type :label
              :v-box/margin {:top 16}
              :style {:-fx-background-color "#0000ff"}
              :text (result-map :text)})
           (state-map :results)))}}})

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc
                (fn [state-val]
                  {:fx/type root :state state-val}))))

(fx/mount-renderer state-atom renderer)

(comment
  (reload-state))