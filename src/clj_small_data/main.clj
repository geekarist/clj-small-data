(ns clj-small-data.main
  (:refer-clojure :exclude [update])
  (:require [clj-small-data.results :as results]
            [clj-small-data.runtime :as runtime]
            [clj-small-data.query :as query]))

(defn init-main [kb-path-str]
  {::mdl:title "Small Data Finder"
   ::mdl:iconified false
   ::mdl:kb-path kb-path-str})

(def init
  (let [kb-path-str "C:/Users/chris/Google Drive/DriveSyncFiles/PERSO-KB"
        got-search-output {::runtime/evt-type ::evt-type:got-search-output}
        got-reinit-request {::runtime/evt-type ::evt-type:got-reinit-request}
        main-init-map (init-main kb-path-str)
        query-init-map (query/init got-search-output got-reinit-request kb-path-str)
        results-init-map results/init]
    (conj main-init-map query-init-map results-init-map)))

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
        (query/view state-map)

        ;; Global buttons
        {:fx/type :button :text "Redraw" :h-box/margin {:left 8}
         :on-action {::runtime/evt-type ::evt-type:redraw-btn-pressed}}
        {:fx/type :button :text "Log" :h-box/margin {:left 4}
         :on-action {::runtime/evt-type ::evt-type:log-btn-pressed}})}

      ;; List of results
      (results/view state-map))}}})

(defmethod runtime/upset ::evt-type:redraw-btn-pressed
  [{:keys [::runtime/coe-state]}]
  {::runtime/eff:state coe-state})

(defmethod runtime/upset ::evt-type:got-reinit-request
  [_arg]
  {::runtime/eff:state init})

(defmethod runtime/upset ::evt-type:got-search-output
  [{:keys [::runtime/coe-state ::runtime/eff:sh:cmd-out]}]
  {::runtime/eff:dispatch
   {::runtime/evt-type ::results/evt-type:search-output-received
    ::runtime/eff:sh:cmd-out eff:sh:cmd-out
    ::results/evt-arg:kb-path (coe-state ::mdl:kb-path)}})

(defmethod runtime/upset ::evt-type:log-btn-pressed
  [{:keys [::runtime/coe-state]}]
  {::runtime/eff:log coe-state})


