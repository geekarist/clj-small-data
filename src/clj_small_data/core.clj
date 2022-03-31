(ns clj-small-data.core
  (:require [cljfx.api :as fx]))

(def finder-init
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
  (atom finder-init))

(defn reload-state! []
  (swap!
   state-atom
   (fn [_state-val]
     finder-init)))

(defn finder-view [{state-map :state dispatch! :dispatch}]
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
         :on-text-changed #(dispatch! [:change-search-query %])}
        {:fx/type :button :text "Search" :h-box/margin {:right 8}}
        {:fx/type :button :text "Reload"
         :on-action (fn [_] (reload-state!))}]}
      (map (fn [result-map]
             {:fx/type :label
              :v-box/margin {:top 16}
              :style {:-fx-background-color "#0000ff"}
              :text (result-map :text)})
           (state-map :results)))}}})

(defn finder-update [state-hash msg-key msg-val]
  (condp = msg-key
    :change-search-query
    (let [new-state-hash (assoc state-hash :search-text msg-val)
          new-effect-vec nil]
      [new-state-hash new-effect-vec])))

(defn dispatch! [[msg-key msg-val :as _message-vec]]
  (let [update-result-vec (finder-update @state-atom msg-key msg-val)
        [new-state-hash _new-effect-vec] update-result-vec
        get-new-state-hash (fn [_current-state-hash] new-state-hash)]
    (swap! state-atom get-new-state-hash)))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc
                (fn [state-val]
                  {:fx/type finder-view :state state-val :dispatch dispatch!}))))

(fx/mount-renderer state-atom renderer)

(comment
  (println @state-atom)
  (reload-state!))