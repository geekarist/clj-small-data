(ns clj-small-data.finder
  (:refer-clojure :exclude [update])
  (:require [clojure.java.shell :as shell]))

(def init
  {:mdl/title "Small Data Finder"
   :mdl/search-text ""
   :mdl/search-field-placeholder "Please enter your search text"
   :mdl/results
   [{:mdl/text "No result found."}]})

(defn view [{state-map :state dispatch! :dispatch}]

  ;; Window
  {:fx/type :stage :showing true :title (state-map :mdl/title)
   :width 600 :height 600

   ;; Main container
   :scene
   {:fx/type :scene :root

    ;; Vertical box
    {:fx/type :v-box :padding 16

     ;; Vertical children
     :children
     (cons

      ;; Query field and buttons
      {:fx/type :h-box
       :children

       [;; Query
        {:fx/type :text-field
         :h-box/hgrow :always :text (state-map :mdl/search-text)
         :prompt-text (state-map :mdl/search-field-placeholder)
         :on-text-changed #(dispatch! [:msg/change-search-query %])}

        ;; Buttons
        {:fx/type :button :text "Clear"
         :on-action (fn [_] (dispatch! [:msg/clear]))}
        {:fx/type :button :text "Search" :h-box/margin {:left 8}
         :on-action (fn [_] (dispatch! [:msg/search]))}
        {:fx/type :button :text "Redraw" :h-box/margin {:left 8}
         :on-action (fn [_] (dispatch! [:msg/redraw]))}
        {:fx/type :button :text "Log"
         :on-action (fn [_] (dispatch! [:msg/log]))}]}

      ;; List of results
      (map (fn [result-map]
             {:fx/type :label :v-box/margin {:top 16}
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
      [new-state-hash new-effect-vec])

    :msg/search
    (let [new-state-hash state-hash
          new-effect-vec
          [:fx/search (state-hash :mdl/search-text)]]
      [new-state-hash new-effect-vec])

    :msg/log
    (let [new-state-hash state-hash
          new-effect-vec [:fx/log state-hash]]
      [new-state-hash new-effect-vec])))

(defn- search-file! [value]
  (println
   (shell/sh
    "rg"
    value "/Volumes/GoogleDrive/My Drive/DriveSyncFiles/PERSO-KB")))

(defn effect! [[key value :as _new-effect-vec] _dispatch!]
  (condp = key
    :fx/search (search-file! value)
    :fx/log (println "State:" value)
    nil nil ; Ignore `nil` effect
    (println "Effect not found:" key)))

