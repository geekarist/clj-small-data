(ns clj-small-data.results
  (:require [clj-small-data.runtime :as runtime]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:import (java.io File)))

(def init
  {::mdl:results []})

(defn view-one-result [result-map]
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
     {::runtime/evt-type ::evt-type:link-clicked
      ::evt-arg (result-map ::mdl:link)}}
    {:fx/type :label
     :v-box/margin {:left 16 :right 16 :top 4 :bottom 16}
     :max-height 100
     :text (result-map ::mdl:text)}]})

(defn view-some-results [results-coll]
  {:fx/type :scroll-pane
   :fit-to-width true
   :content {:fx/type :v-box
             :padding {:top 16}
             :children
             (map view-one-result results-coll)}})

(defn view-empty-results []
  {:fx/type :label
   :style {:-fx-border-color ["#aaaaaa" "#00000000" "#00000000" "#00000000"]
           :-fx-border-width 0.5}
   :max-width Double/MAX_VALUE
   :padding {:top 16 :bottom 16 :left 16 :right 16}
   :text "No search results."})

(defn view [state-map]
  (let [results-coll (state-map ::mdl:results)]
    (if (not-empty results-coll)
      (view-some-results results-coll)
      (view-empty-results))))

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
        path-wrapped-str path-str
        file-name-str (when path-str (last (str/split path-str #"[/\\]")))
        name-str (when file-name-str (str/replace file-name-str #"\.md$" ""))
        lines-map (some-> data-map :lines)
        md-str (some-> lines-map :text)
        #_{html-str (mdc/md-to-html-string md-str)}]
    (when (= type-str "match")
      {::mdl:name name-str
       ::mdl:path path-wrapped-str
       ::mdl:link (path->uri kb-path-str path-str)
       ::mdl:line-number 123
       ::mdl:text md-str})))

(defmethod runtime/upset ::evt-type:search-output-received
  [{:keys [::runtime/coe-state ::runtime/eff:sh:cmd-out ::evt-arg:kb-path]}]
  {::runtime/eff:state
   (let [kb-path-str evt-arg:kb-path
         split-output-json-vec (str/split eff:sh:cmd-out #"\n")
         result-coll (map (fn [json-str] (json->result kb-path-str json-str))
                          split-output-json-vec)
         non-nil-result-coll (filter some? result-coll)]
     (assoc coe-state ::mdl:results non-nil-result-coll))})

(defmethod runtime/upset ::evt-type:link-clicked
  [{:keys [::evt-arg]}]
  {::runtime/eff:open-uri evt-arg})