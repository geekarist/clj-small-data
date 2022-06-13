(ns clj-small-data.finder
  (:refer-clojure :exclude [update])
  (:require [clj-small-data.results :as results]
            [clj-small-data.runtime :as runtime]))

(def init
  (conj {::mdl:title "Small Data Finder"
         ::mdl:iconified false
         ::mdl:kb-path "C:/Users/chris/Google Drive/DriveSyncFiles/PERSO-KB"
         ::mdl:search-text ""
         ::mdl:search-field-placeholder "Please enter your search text"}
        results/init))

(defn view-query [state-map]
  [{:fx/type :text-field
    :h-box/hgrow :always :text (state-map ::mdl:search-text)
    :prompt-text (state-map ::mdl:search-field-placeholder)
    :on-text-changed {::runtime/evt-type ::evt-type:change-search-query}}
   {:fx/type :button :text "Clear" :h-box/margin {:left 8}
    :on-action {::runtime/evt-type ::evt-type:clear-btn-pressed}}
   {:fx/type :button :text "Find" :h-box/margin {:left 4}
    :on-action {::runtime/evt-type ::evt-type:search-btn-pressed}}])

(defn view [state-map]

  ;; Window
  {:fx/type :stage :showing true :title (state-map ::mdl:title)
   :iconified (state-map ::mdl:iconified)
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

       (conj
        ;; Query
        (view-query state-map)

        ;; Global buttons
        {:fx/type :button :text "Redraw" :h-box/margin {:left 8}
         :on-action {::runtime/evt-type ::evt-type:redraw-btn-pressed}}
        {:fx/type :button :text "Log" :h-box/margin {:left 4}
         :on-action {::runtime/evt-type ::evt-type:log-btn-pressed}})}

      ;; List of results
      (results/view state-map))}}})

(defmethod runtime/upset ::evt-type:change-search-query
  [{:keys [::runtime/coe-state fx/event]}]
  {::runtime/eff:state (assoc coe-state ::mdl:search-text event)})

(defmethod runtime/upset ::evt-type:clear-btn-pressed
  [_arg]
  {::runtime/eff:state init})

(defmethod runtime/upset ::evt-type:search-btn-pressed
  [{:keys [::runtime/coe-state]}]
  (let [kb-path-str (coe-state ::mdl:kb-path)
        query-str (coe-state ::mdl:search-text)
        cmd-vec ["rg" "--json" query-str kb-path-str]
        got-output-event {::runtime/evt-type ::evt-type:search-output-received}
        eff-arg-map {::runtime/eff:sh:cmd cmd-vec
                     ::runtime/eff:sh:got-output got-output-event}
        upset-result-map {::runtime/eff:sh eff-arg-map}]
    upset-result-map))

(defmethod runtime/upset ::evt-type:redraw-btn-pressed
  [{:keys [::runtime/coe-state]}]
  {::runtime/eff:state coe-state})

(defmethod runtime/upset ::evt-type:search-output-received
  [{:keys [::runtime/coe-state ::runtime/eff:sh:cmd-out]}]
  {::runtime/eff:dispatch
   {::runtime/evt-type ::results/evt-type:search-output-received
    ::runtime/eff:sh:cmd-out eff:sh:cmd-out
    ::results/evt-arg:kb-path (coe-state ::mdl:kb-path)}})

(defmethod runtime/upset ::evt-type:log-btn-pressed
  [{:keys [::runtime/coe-state]}]
  {::runtime/eff:log coe-state})


