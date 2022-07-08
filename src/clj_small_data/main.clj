(ns clj-small-data.main
  (:refer-clojure :exclude [update])
  (:require [clj-small-data.query :as query]
            [clj-small-data.results :as results]
            [clj-small-data.runtime :as runtime]
            [cljfx.api :as fx]))

(defn init-main [kb-path-str]
  {::model|title "Small Data Finder"
   ::model|iconified false
   ::model|kb-path kb-path-str
   ::model|status "Idle"})

(def init
  (let [kb-path-str "C:/Users/chris/Google Drive/DriveSyncFiles/PERSO-KB"
        on-result-received {::runtime/event-type ::event-type|on-results-received}
        on-reinit-request {::runtime/event-type ::event-type|on-reinit-request}
        on-send-query {::runtime/event-type ::event-type|on-status-changed
                       ::event-arg|new-status "Searching..."}
        on-receive-results {::runtime/event-type ::event-type|on-status-changed
                            ::event-arg|new-status "Idle"}
        main-init-map (init-main kb-path-str)
        query-init-map (query/init kb-path-str on-result-received on-reinit-request on-send-query)
        results-init-map (results/init on-receive-results)]
    (conj main-init-map query-init-map results-init-map)))

(defn- view [sub-state]
  {:fx/type :stage ; Window
   :showing true :title (sub-state ::model|title)
   :iconified (sub-state ::model|iconified)
   :width 600 :height 600

   :scene
   {:fx/type :scene ; Main container

    :root
    {:fx/type :v-box ; Vertical box
     :fill-width true
     :children
     (vector

      {:fx/type :h-box ; Query field and buttons
       :padding 16
       :alignment :center
       :children

       (conj
        ;; Query
        (query/view sub-state)

        ;; Global buttons
        {:fx/type :button :text "Redraw" :h-box/margin {:left 8}
         :on-action {::runtime/event-type ::event-type|redraw-btn-pressed}}
        {:fx/type :button :text "Log" :h-box/margin {:left 4}
         :on-action {::runtime/event-type ::event-type|log-btn-pressed}}
        {:fx/type :label :text (sub-state ::model|status)
         :alignment :center-right :h-box/margin {:left 8}
         :pref-width 70 :max-width 70})}

      ;; List of results
      (results/view sub-state))}}})

(defn view-ctx [{context-obj :fx/context}]
  (let [get-state #(fx/sub-val context-obj %)]
    (view get-state)))

(defmethod runtime/upset ::event-type|redraw-btn-pressed
  [{state-map ::runtime/coeffect|state}]
  {::runtime/effect|state state-map})

(defmethod runtime/upset ::event-type|on-reinit-request
  [_arg]
  {::runtime/effect|state init})

(defmethod runtime/upset ::event-type|on-results-received
  [{state-map ::runtime/coeffect|state
    cmd-out-str ::runtime/effect|sh|cmd-out}]

  {::runtime/effect|dispatch
   {::runtime/event-type ::event-type|on-status-changed
    ::event-arg|new-status "Presenting..."

    ::then-dispatch {::runtime/event-type ::results/event-type|search-output-received
                     ::runtime/effect|sh|cmd-out cmd-out-str
                     ::results/event-arg|kb-path (state-map ::model|kb-path)}}})

(defmethod runtime/upset ::event-type|log-btn-pressed
  [{state-map ::runtime/coeffect|state}]
  {::runtime/effect|log state-map})

(defmethod runtime/upset ::event-type|on-status-changed
  [{new-status-str ::event-arg|new-status
    state-map ::runtime/coeffect|state
    next-event-map ::then-dispatch}]

  (let [new-state-map (assoc state-map ::model|status new-status-str)
        state-effect-map {::runtime/effect|state new-state-map}
        next-event-effect-map (when next-event-map {::runtime/effect|dispatch next-event-map})]
    (conj state-effect-map next-event-effect-map)))