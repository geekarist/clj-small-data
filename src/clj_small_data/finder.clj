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
    {:fx/type :v-box
     :children
     (vector

      ;; Query field and buttons
      {:fx/type :h-box
       :padding 16
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
      {:fx/type :scroll-pane
       :content

       {:fx/type :v-box
        :children
        (map (fn [result-map]
               {:fx/type :v-box
                :v-box/margin {:left 16
                               :right 16
                               :bottom 16}
                :style {:-fx-border-color "#aaaaaa"
                        :-fx-border-width 1}
                :children
                [{:fx/type :label
                  :v-box/margin {:left 16 :right 16 :top 16 :bottom 4}
                  :text (result-map :mdl/name)}
                 {:fx/type :label
                  :v-box/margin {:left 16 :right 16 :top 4 :bottom 4}
                  :text (result-map :mdl/path)}
                 {:fx/type :label
                  :v-box/margin {:left 16 :right 16 :top 4 :bottom 4}
                  :text (result-map :mdl/link)}
                 {:fx/type :label
                  :v-box/margin {:left 16 :right 16 :top 4 :bottom 16}
                  :text (result-map :mdl/text)}]})
             (state-map :mdl/results))}})}}})

(defn- wrap-long-lines
  "Take `string` and insert line endings every `width` characters."
  [string width]
  (loop [i 0
         acc ""]
    (letfn [(out-of-bounds? [string i]
              (>= i (.length string)))
            (next-index-fn [i] (inc i))
            (mult? [width i]
              (and (not= 0 i)
                   (= 0 (mod i width))))
            (next-acc-fn [string width i acc]
              (str acc
                   (nth string i)
                   (if (mult? width i) "\n" "")))]
      (if (out-of-bounds? string i)
        acc
        (recur (next-index-fn i)
               (next-acc-fn string width i acc))))))

(comment
  (wrap-long-lines (str/join (take 10000 (repeat "0123456789"))) 10))

(defn- json->result [json-str]
  {:mdl/name "TODO"
   :mdl/path "C:/Users/chris/TODO.md"
   :mdl/link "obsidian://TODO"
   :mdl/line-number 123
   :mdl/text (wrap-long-lines json-str 100)})

(defn- new-state-on-search-output-received [state-hash search-output-json-str]
  (let [split-output-json-vec (str/split search-output-json-str #"\n")
        results (map json->result split-output-json-vec)]
    (assoc state-hash
           :mdl/results results)))

(defn update [state-hash event-key event-val]

  (condp = event-key

    :evt/change-search-query
    (let [new-state-hash (assoc state-hash :mdl/search-text event-val)
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

    :evt/search-output-received
    (let [new-state-hash
          (new-state-on-search-output-received state-hash event-val)]
      [new-state-hash nil])

    :evt/log-btn-pressed
    (let [new-state-hash state-hash
          new-effect-vec [:eff/log state-hash]]
      [new-state-hash new-effect-vec])

    (do (println "Unknown message key:" event-key)
        [state-hash nil])))

;; (def SEARCH_DIR "/Volumes/GoogleDrive/My Drive/DriveSyncFiles/PERSO-KB")
(def SEARCH_DIR "C:/Users/chris/Google Drive/DriveSyncFiles/PERSO-KB")

(defn- search-file! [query dispatch!]
  (future
    (let [result (shell/sh "rg" "--json" query SEARCH_DIR)
          output (result :out)]
      (fx/on-fx-thread
       (dispatch! [:evt/search-output-received output])))))

(comment
  (search-file! "hello" #(println (nth % 1))))

(defn effect! [[key value :as _new-effect-vec] dispatch!]
  (condp = key
    :eff/search (search-file! value dispatch!)
    :eff/log (println "State:" value)
    nil nil ; Ignore `nil` effect
    (println "Effect not found:" key)))

