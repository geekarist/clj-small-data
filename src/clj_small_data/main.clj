(ns clj-small-data.main
  (:refer-clojure :exclude [update])
  (:require [clj-small-data.query :as query]
            [clj-small-data.results :as results]
            [clj-small-data.runtime :as runtime]
            [cljfx.api :as fx]))

(defn view [{context :fx/context}]
  (println "Executing main view")
  {:fx/type :stage ; Window
   :showing true :title (fx/sub-val context ::model|title)
   :iconified (fx/sub-val context ::model|iconified)
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
       :alignment :center-left
       :children

       (conj
        ;; Query
        [{:fx/type query/view
          :h-box/hgrow :always}]

        ;; Global buttons
        {:fx/type :button :text "Redraw" :h-box/margin {:left 8}
         :on-action {::runtime/event-type ::event-type|redraw-btn-pressed}}
        {:fx/type :button :text "Log" :h-box/margin {:left 4}
         :on-action {::runtime/event-type ::event-type|log-btn-pressed}}
        {:fx/type :label :text (fx/sub-val context ::model|status)
         :alignment :center-right :h-box/margin {:left 8}
         :pref-width 70 :max-width 70})}

      ;; List of results
      {:fx/type results/view
       :v-box/vgrow :always})}}})

(defmethod runtime/upset ::event-type|init
  [{context :fx/context}]

  (let [kb-path-str "C:/Users/chris/Google Drive/DriveSyncFiles/PERSO-KB"
        on-result-received {::runtime/event-type ::event-type|on-results-received}
        on-reinit-request {::runtime/event-type ::event-type|on-reinit-request}
        on-send-query {::runtime/event-type ::event-type|on-status-changed
                       ::event-arg|new-status "Searching..."}
        on-receive-results {::runtime/event-type ::event-type|on-status-changed
                            ::event-arg|new-status "Idle"}]

    {:context (fx/reset-context ; Create a new context as this is the main module
               context
               {::model|title "Small Data Finder"
                ::model|iconified false
                ::model|kb-path kb-path-str
                ::model|status "Idle"})

     ::runtime/effect|dispatches
     [{::runtime/event-type ::query/event-type|init
       ::query/event-args [kb-path-str on-result-received on-reinit-request on-send-query]}
      {::runtime/event-type ::results/event-type|init
       ::results/event-args [on-receive-results]}]}))

(defmethod runtime/upset ::event-type|redraw-btn-pressed
  [{context :fx/context}]
  {:context context})

(defmethod runtime/upset ::event-type|on-reinit-request
  [{_context :fx/context}]
  {:dispatch {::runtime/event-type ::event-type|init}})

(defmethod runtime/upset ::event-type|on-results-received
  [{context :fx/context
    cmd-out-str ::runtime/effect|sh|cmd-out}]

  {::runtime/effect|dispatch
   {::runtime/event-type ::event-type|on-status-changed
    ::event-arg|new-status "Presenting..."

    ::then-dispatch {::runtime/event-type ::results/event-type|search-output-received
                     ::runtime/effect|sh|cmd-out cmd-out-str
                     ::results/event-arg|kb-path (fx/sub-val context ::model|kb-path)}}})

(defmethod runtime/upset ::event-type|log-btn-pressed
  [{context :fx/context}]
  {::runtime/effect|log (fx/sub-val context identity)})

(defmethod runtime/upset ::event-type|on-status-changed
  [{new-status-str ::event-arg|new-status
    context :fx/context
    next-event-map ::then-dispatch}]

  (let [new-context (fx/swap-context context assoc ::model|status new-status-str)
        state-effect-map {:context new-context}
        next-event-effect-map (when next-event-map {::runtime/effect|dispatch next-event-map})]
    (conj state-effect-map next-event-effect-map)))