(ns clj-small-data.query
  (:require [clj-small-data.runtime :as runtime]))

(def init-map
  {::mdl:search-text ""
   ::mdl:search-field-placeholder "Please enter your search text"})

(defn init
  [on-search-output-received on-reinit-request kb-path-str on-send-query]
  (conj init-map
        {::on-search-output-received on-search-output-received
         ::mdl:on-reinit-request on-reinit-request
         ::mdl:kb-path kb-path-str
         ::mdl:on-send-query on-send-query}))

(defn view [state-map]
  [{:fx/type :text-field
    :h-box/hgrow :always :text (state-map ::mdl:search-text)
    :prompt-text (state-map ::mdl:search-field-placeholder)
    :on-text-changed {::runtime/evt-type ::evt-type:change-search-query}}
   {:fx/type :button :text "Clear" :h-box/margin {:left 8}
    :on-action {::runtime/evt-type ::evt-type:clear-btn-pressed}}
   {:fx/type :button :text "Find" :h-box/margin {:left 4}
    :on-action {::runtime/evt-type ::evt-type:search-btn-pressed}}])

(defmethod runtime/upset ::evt-type:change-search-query
  [{:keys [::runtime/coe-state fx/event]}]
  {::runtime/eff:state (assoc coe-state ::mdl:search-text event)})

(defmethod runtime/upset ::evt-type:clear-btn-pressed
  [{:keys [::runtime/coe-state]}]
  {::runtime/eff:dispatch (coe-state ::mdl:on-reinit-request)})

(defmethod runtime/upset ::evt-type:search-btn-pressed
  [{:keys [::runtime/coe-state]}]
  (let [kb-path-str (coe-state ::mdl:kb-path)
        query-str (coe-state ::mdl:search-text)
        cmd-vec ["rg" "--json" "--glob" "**/*.md" query-str kb-path-str]
        got-output-event (coe-state ::on-search-output-received)
        eff-arg-map {::runtime/eff:sh:cmd cmd-vec
                     ::runtime/eff:sh:got-output got-output-event}
        on-send-query (coe-state ::mdl:on-send-query)
        upset-result-map {::runtime/eff:dispatch on-send-query
                          ::runtime/eff:sh eff-arg-map}]
    upset-result-map))

