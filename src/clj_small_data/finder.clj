(ns clj-small-data.finder
  (:refer-clojure :exclude [update]))

(def init
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

(defn view [{state-map :state dispatch! :dispatch}]
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
         :prompt-text (state-map :search-field-placeholder)
         :on-text-changed #(dispatch! [:msg/change-search-query %])}
        {:fx/type :button :text "Search" :h-box/margin {:right 8}}
        {:fx/type :button :text "Reload"
         :on-action (fn [_] (dispatch! [:msg/reload-initial-state]))}]}
      (map (fn [result-map]
             {:fx/type :label
              :v-box/margin {:top 16}
              :style {:-fx-background-color "#0000ff"}
              :text (result-map :text)})
           (state-map :results)))}}})

(defn update [state-hash msg-key msg-val]
  (condp = msg-key
    :msg/change-search-query
    (let [new-state-hash (assoc state-hash :search-text msg-val)
          new-effect-vec nil]
      [new-state-hash new-effect-vec])
    :msg/reload-initial-state
    (let [new-state-hash init
          new-effect-vec nil]
      [new-state-hash new-effect-vec])))

(defn effect! [[key _value :as _new-effect-vec] _dispatch!]
  #_(condp = key
      :fx/reload-initial-state
      (reload-state!)))

