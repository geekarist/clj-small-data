(ns clj-small-data.query
  (:require [clj-small-data.runtime :as runtime]
            [cljfx.api :as fx]))

(def init-map
  {::model|search-text ""
   ::model|search-field-placeholder "Please enter your search text"})

(defn init
  [kb-path-str on-search-output-received on-reinit-request on-send-query]
  (conj init-map
        {::model|kb-path kb-path-str
         ::model|on-send-query on-send-query
         ::model|on-reinit-request on-reinit-request
         ::model|on-search-output on-search-output-received}))

(defn view [sub _desc]
  {:fx/type :h-box
   :children [{:fx/type :text-field
               :h-box/hgrow :always :text (sub ::model|search-text)
               :prompt-text (sub ::model|search-field-placeholder)
               :on-text-changed {::runtime/event-type ::event-type|change-search-query}}
              {:fx/type :button :text "Clear" :h-box/margin {:left 8}
               :on-action {::runtime/event-type ::event-type|clear-btn-pressed}}
              {:fx/type :button :text "Find" :h-box/margin {:left 4}
               :on-action {::runtime/event-type ::event-type|search-btn-pressed}}]})

(defmethod runtime/upset ::event-type|change-search-query
  [{state-map ::runtime/coeffect|state
    search-str :fx/event}]
  {::runtime/effect|state (assoc state-map ::model|search-text search-str)})

(defmethod runtime/upset ::event-type|clear-btn-pressed
  [{state-map ::runtime/coeffect|state}]
  {::runtime/effect|dispatch (state-map ::model|on-reinit-request)})

(defmethod runtime/upset ::event-type|search-btn-pressed
  [{{kb-path-str ::model|kb-path
     query-str ::model|search-text
     on-search-output ::model|on-search-output
     on-send-query ::model|on-send-query} ::runtime/coeffect|state}]

  {::runtime/effect|dispatch on-send-query
   ::runtime/effect|sh {::runtime/effect|sh|cmd
                        ["rg" "--json" "--glob" "**/*.md" query-str kb-path-str]
                        ::runtime/effect|sh|on-command-output on-search-output}})

