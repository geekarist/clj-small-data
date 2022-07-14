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

(defn view [{context :fx/context}]
  (println "Executing query view")
  {:fx/type :h-box
   :children [{:fx/type :text-field
               :h-box/hgrow :always :text (fx/sub-val context ::model|search-text)
               :prompt-text (fx/sub-val context ::model|search-field-placeholder)
               :on-text-changed {::runtime/event-type ::event-type|change-search-query}}
              {:fx/type :button :text "Clear" :h-box/margin {:left 8}
               :on-action {::runtime/event-type ::event-type|clear-btn-pressed}}
              {:fx/type :button :text "Find" :h-box/margin {:left 4}
               :on-action {::runtime/event-type ::event-type|search-btn-pressed}}]})

(defmethod runtime/upset ::event-type|change-search-query
  [{search-str :fx/event
    context :fx/context}]
  {:context (fx/swap-context context assoc ::model|search-text search-str)})

(defmethod runtime/upset ::event-type|clear-btn-pressed
  [{context :fx/context}]
  {::runtime/effect|dispatch (fx/sub-val context ::model|on-reinit-request)})

(defmethod runtime/upset ::event-type|search-btn-pressed
  [{context :fx/context}]

  (let [kb-path-str (fx/sub-val context ::model|kb-path)
        query-str (fx/sub-val context ::model|search-text)
        on-search-output (fx/sub-val context ::model|on-search-output)
        on-send-query (fx/sub-val context ::model|on-send-query)]

    {::runtime/effect|dispatch on-send-query
     ::runtime/effect|sh {::runtime/effect|sh|cmd
                          ["rg" "--json" "--glob" "**/*.md" query-str kb-path-str]
                          ::runtime/effect|sh|on-command-output on-search-output}}))

