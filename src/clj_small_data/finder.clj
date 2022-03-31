(ns clj-small-data.finder
  (:refer-clojure :exclude [update]))

(def init
  {:mdl/title "Small Data Finder"
   :mdl/search-text ""
   :mdl/search-field-placeholder "Please enter your search text"
   :mdl/results
   [{:mdl/text "No result found."}]})

(defn view [{state-map :state dispatch! :dispatch}]
  {:fx/type :stage :showing true :title (state-map :mdl/title)
   :width 600 :height 600
   :scene
   {:fx/type :scene :root
    {:fx/type :v-box :padding 16 :style {:-fx-background-color "#ffff00"}
     :children
     (cons
      {:fx/type :h-box :style {:-fx-background-color "#00ff00"}
       :children
       [{:fx/type :text-field :style {:-fx-background-color "#ff0000"}
         :h-box/hgrow :always :h-box/margin {:right 8}
         :text (state-map :mdl/search-text)
         :prompt-text (state-map :mdl/search-field-placeholder)
         :on-text-changed #(dispatch! [:msg/change-search-query %])}
        {:fx/type :button :text "Search"}
        {:fx/type :button :text "Redraw" :h-box/margin {:left 8}
         :on-action (fn [_] (dispatch! [:msg/redraw]))}
        {:fx/type :button :text "Clear" :h-box/margin {:left 8}
         :on-action (fn [_] (dispatch! [:msg/clear]))}]}
      (map (fn [result-map]
             {:fx/type :label :v-box/margin {:top 16}
              :style {:-fx-background-color "#0000ff"}
              :text (result-map :mdl/text)})
           (state-map :mdl/results)))}}})

(defn update [state-hash msg-key msg-val]
  (condp = msg-key
    :msg/change-search-query
    (let [new-state-hash (assoc state-hash :mdl/search-text msg-val)
          new-effect-vec nil]
      [new-state-hash new-effect-vec])
    :msg/redraw
    (let [new-state-hash state-hash
          new-effect-vec nil]
      [new-state-hash new-effect-vec])
    :msg/clear
    (let [new-state-hash init
          new-effect-vec nil]
      [new-state-hash new-effect-vec])))

(defn effect! [[key _value :as _new-effect-vec] _dispatch!]
  #_(condp = key
      :fx/reload-initial-state
      (reload-state!)))

