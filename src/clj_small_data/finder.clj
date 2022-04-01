(ns clj-small-data.finder
  (:refer-clojure :exclude [update])
  (:require [cljfx.api :as fx]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(def init
  {:mdl/title "Small Data Finder"
   :mdl/search-text ""
   :mdl/search-field-placeholder "Please enter your search text"
   :mdl/results
   []})

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
         :on-text-changed #(dispatch! [:evt/change-search-query %])}

        ;; Buttons
        {:fx/type :button :text "Clear"
         :on-action (fn [_] (dispatch! [:evt/clear-btn-pressed]))}
        {:fx/type :button :text "Search" :h-box/margin {:left 8}
         :on-action (fn [_] (dispatch! [:evt/search-btn-pressed]))}
        {:fx/type :button :text "Redraw" :h-box/margin {:left 8}
         :on-action (fn [_] (dispatch! [:evt/redraw-btn-pressed]))}
        {:fx/type :button :text "Log"
         :on-action (fn [_] (dispatch! [:evt/log-btn-pressed]))}]}

      ;; List of results
      (map (fn [result-map]
             {:fx/type :label :v-box/margin {:top 16}
              :padding 16
              :style {:-fx-border-color "#aaaaaa"
                      :-fx-border-width 1}
              :text (str (result-map :mdl/file) "\n" (result-map :mdl/match))})
           (state-map :mdl/results)))}}})

(defn- str->result [string]
  (let [split-by-colon-vec (str/split string #":")
        [_disk-str path-str match-str] split-by-colon-vec
        split-path-by-slash (str/split path-str #"/")
        short-path-str (last split-path-by-slash)]
    {:mdl/file short-path-str
     :mdl/match match-str}))

(defn- update-on-receive-search-output [state-hash search-output-str]
  (let [split-output-vec (str/split search-output-str #"\n")
        results (map str->result split-output-vec)]
    (assoc state-hash :mdl/results results)))

(defn update [state-hash msg-key msg-val]

  (condp = msg-key

    :evt/change-search-query
    (let [new-state-hash (assoc state-hash :mdl/search-text msg-val)
          new-effect-vec nil]
      [new-state-hash new-effect-vec])

    :evt/redraw-btn-pressed
    (let [new-state-hash state-hash
          new-effect-vec nil]
      [new-state-hash new-effect-vec])

    :evt/clear-btn-pressed
    (let [new-state-hash init
          new-effect-vec nil]
      [new-state-hash new-effect-vec])

    :evt/search-btn-pressed
    (let [new-state-hash state-hash
          new-effect-vec
          [:eff/search (state-hash :mdl/search-text)]]
      [new-state-hash new-effect-vec])

    :evt/receive-search-output
    (let [new-state-hash
          (update-on-receive-search-output state-hash msg-val)]
      [new-state-hash nil])

    :evt/log-btn-pressed
    (let [new-state-hash state-hash
          new-effect-vec [:eff/log state-hash]]
      [new-state-hash new-effect-vec])

    (do (println "Unknown message key:" msg-key)
        [state-hash nil])))

;; (def SEARCH_DIR "/Volumes/GoogleDrive/My Drive/DriveSyncFiles/PERSO-KB")
(def SEARCH_DIR "C:/Users/chris/Google Drive/DriveSyncFiles/PERSO-KB")

(defn- search-file! [query dispatch!]
  (future
    (let [result (shell/sh "rg" query SEARCH_DIR)
          output (result :out)]
      (fx/on-fx-thread
       (dispatch! [:evt/receive-search-output output])))))

(defn effect! [[key value :as _new-effect-vec] dispatch!]
  (condp = key
    :eff/search (search-file! value dispatch!)
    :eff/log (println "State:" value)
    nil nil ; Ignore `nil` effect
    (println "Effect not found:" key)))

