(ns clj-small-data.query
  (:require [clj-small-data.runtime :as runtime]))

(def init-map
  {::mdl:search-text ""
   ::mdl:search-field-placeholder "Please enter your search text"})

(defn init
  [kb-path-str on-search-output-received on-reinit-request on-send-query]
  (conj init-map
        {::mdl:kb-path kb-path-str
         ::mdl:on-send-query on-send-query
         ::mdl:on-reinit-request on-reinit-request
         ::on-search-output-received on-search-output-received}))

(defn view [state-map]
  [{:fx/type :text-field
    :h-box/hgrow :always :text (state-map ::mdl:search-text)
    :prompt-text (state-map ::mdl:search-field-placeholder)
    :on-text-changed {::runtime/event-type ::event-type:change-search-query}}
   {:fx/type :button :text "Clear" :h-box/margin {:left 8}
    :on-action {::runtime/event-type ::event-type:clear-btn-pressed}}
   {:fx/type :button :text "Find" :h-box/margin {:left 4}
    :on-action {::runtime/event-type ::event-type:search-btn-pressed}}])

(defmethod runtime/upset ::event-type:change-search-query
  [{:keys [::runtime/coeffect|state fx/event]}]
  {::runtime/effect|state (assoc coeffect|state ::mdl:search-text event)})

(defmethod runtime/upset ::event-type:clear-btn-pressed
  [{:keys [::runtime/coeffect|state]}]
  {::runtime/effect|dispatch (coeffect|state ::mdl:on-reinit-request)})

(defmethod runtime/upset ::event-type:search-btn-pressed
  [{:keys [::runtime/coeffect|state]}]
  (let [kb-path-str (coeffect|state ::mdl:kb-path)
        query-str (coeffect|state ::mdl:search-text)
        cmd-vec ["rg" "--json" "--glob" "**/*.md" query-str kb-path-str]
        got-output-event (coeffect|state ::on-search-output-received)
        eff-arg-map {::runtime/effect|sh|cmd cmd-vec
                     ::runtime/effect|sh|on-command-output got-output-event}
        on-send-query (coeffect|state ::mdl:on-send-query)
        upset-result-map {::runtime/effect|dispatch on-send-query
                          ::runtime/effect|sh eff-arg-map}]
    upset-result-map))

