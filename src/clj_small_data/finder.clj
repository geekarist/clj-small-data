(ns clj-small-data.finder
  (:refer-clojure :exclude [update])
  #_{:clj-kondo/ignore [:unused-namespace]}
  (:require
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.data.json :as json]
   [taoensso.timbre :as timbre]
   [clj-uri.core :as curi]
   [clj-small-data.runtime :as runtime])
  (:import (java.io File)))

(def init
  {::mdl:title "Small Data Finder"
   ::mdl:iconified false
   ::mdl:kb-path "C:/Users/chris/Google Drive/DriveSyncFiles/PERSO-KB"
   ::mdl:search-text ""
   ::mdl:search-field-placeholder "Please enter your search text"
   ::mdl:results
   []})

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

       [;; Query
        {:fx/type :text-field
         :h-box/hgrow :always :text (state-map ::mdl:search-text)
         :prompt-text (state-map ::mdl:search-field-placeholder)
         :on-text-changed {::evt-type ::evt-type:change-search-query}}

        ;; Buttons
        {:fx/type :button :text "Clear" :h-box/margin {:left 8}
         :on-action {::evt-type ::evt-type:clear-btn-pressed}}
        {:fx/type :button :text "Find" :h-box/margin {:left 4}
         :on-action {::evt-type ::evt-type:search-btn-pressed}}
        {:fx/type :button :text "Redraw" :h-box/margin {:left 8}
         :on-action {::evt-type ::evt-type:redraw-btn-pressed}}
        {:fx/type :button :text "Log" :h-box/margin {:left 4}
         :on-action {::evt-type ::evt-type:log-btn-pressed}}]}

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
                  :text (result-map ::mdl:name)}
                 {:fx/type :label
                  :v-box/margin {:left 16 :right 16 :top 4 :bottom 4}
                  :text (result-map ::mdl:path)}
                 {:fx/type :hyperlink
                  :v-box/margin {:left 16 :right 16 :top 4 :bottom 4}
                  :text (result-map ::mdl:link)
                  :on-action
                  {::evt-type ::evt-type:link-clicked
                   ::evt-arg (result-map ::mdl:link)}}
                 {:fx/type :label
                  :v-box/margin {:left 16 :right 16 :top 4 :bottom 16}
                  :text (result-map ::mdl:text)}]})
             (state-map ::mdl:results))}})}}})

(defn- wrap-long-lines
  "Take `string` and insert line endings every `width` characters."
  [string width]
  (when string
    (loop [i 0
           acc ""]
      (letfn [(out-of-bounds? [string i]
                (>= i (.length string)))
              (mult? [width i]
                (and (not= 0 i)
                     (= 0 (mod i width))))
              (next-acc-fn [string width i acc]
                (str acc
                     (nth string i)
                     (if (mult? width i) "\n" "")))]
        (if (out-of-bounds? string i)
          acc
          (recur (inc i)
                 (next-acc-fn string width i acc)))))))

(defn- path->uri [kb-path-str path-str]
  ;; kb-path-str: "c:\a\b\c-kb"
  ;; path-str: "c:\a\b\c-kb\ab cd.md"
  ;; result: "obsidian://open?vault=c-kb&file=ab%20cd.md"
  (let [vault-str (-> kb-path-str File. .getName)
        file-str (-> path-str File. .getName)
        file-no-ext-str (str/replace file-str #".md$" "")
        file-encoded-str (java.net.URLEncoder/encode file-no-ext-str "UTF-8")
        file-encoded-fixed-str (str/replace file-encoded-str "+" "%20")
        uri-pattern "obsidian://open?vault=%s&file=%s"
        uri (format uri-pattern vault-str file-encoded-fixed-str)]
    uri))

(defn- json->result [kb-path-str json-str]
  (let [json-deserialized (json/read-str json-str :key-fn keyword)
        type-str (some-> json-deserialized :type)
        data-map (some-> json-deserialized :data)
        path-map (some-> data-map :path)
        path-str (some-> path-map :text)
        path-wrapped-str (wrap-long-lines path-str 80)
        file-name-str (when path-str (last (str/split path-str #"[/\\]")))
        name-str (when file-name-str (str/replace file-name-str #"\.md$" ""))
        lines-map (some-> data-map :lines)
        text-str (some-> lines-map
                         :text
                         (wrap-long-lines 80))]
    (when (= type-str "match")
      {::mdl:name name-str
       ::mdl:path path-wrapped-str
       ::mdl:link (path->uri kb-path-str path-str)
       ::mdl:line-number 123
       ::mdl:text text-str})))

(defn- new-state-on-search-output-received [state-map search-output-json-str]
  (let [split-output-json-vec (str/split search-output-json-str #"\n")
        kb-path-str (state-map ::mdl:kb-path)
        mdl-item-map (map (fn [json-str] (json->result kb-path-str json-str))
                          split-output-json-vec)
        results (filter some? mdl-item-map)]
    (assoc state-map ::mdl:results results)))

(defmulti upset ::evt-type)

(defmethod upset ::evt-type:change-search-query
  [{:keys [::runtime/coe-state fx/event]}]
  (println "Search query changed:" event)
  {::runtime/eff:state (assoc coe-state ::mdl:search-text event)})

(defmethod upset ::evt-type:redraw-btn-pressed
  [{:keys [::runtime/coe-state]}]
  {::runtime/eff:state coe-state})

(defmethod upset ::evt-type:clear-btn-pressed
  [_arg]
  {::runtime/eff:state init})

(defmethod upset ::evt-type:search-btn-pressed
  [{:keys [::runtime/coe-state]}]
  {::eff:search [(coe-state ::mdl:kb-path) (coe-state ::mdl:search-text)]})

(defmethod upset ::evt-type:search-output-received
  [{:keys [::runtime/coe-state ::evt-arg]}]
  (println "Event:" evt-arg)
  {::runtime/eff:state
   (new-state-on-search-output-received coe-state evt-arg)})

(defmethod upset ::evt-type:log-btn-pressed
  [{:keys [::runtime/coe-state]}]
  {::runtime/eff:log coe-state})

(defmethod upset ::evt-type:link-clicked
  [{:keys [::evt-arg]}]
  {::eff:open-uri evt-arg})

(defn- search-file! [[search-dir query] dispatch!]
  (println (format "Searching into %s query %s" search-dir query))
  (future
    (let [result (shell/sh "rg" "--json" query search-dir)
          _ (println (format "Search result: %s" result))
          output (result :out)
          _ (println (format "Standard output: %s" output))]
      (dispatch! {::evt-type ::evt-type:search-output-received ::evt-arg output}))))

(def effects
  {::eff:search (fn [value dispatch!] (search-file! value dispatch!))
   ::eff:open-uri (fn [value _dispatch!] (curi/open! value))})

(def coeffects {})