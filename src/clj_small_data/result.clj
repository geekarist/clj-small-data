(ns clj-small-data.result
  (:require [clj-small-data.runtime :as runtime]
            [clojure.data.json :as json]
            [clojure.string :as str]))

; ----------------------------------- Model ---------------------------------- ;

(comment "Model: single search result"
         {::model|name "file-name" ; File name without folder or extension
          ::model|path "/file/dir/file-path.md" ; Full file path
          ::model|link "obsidian://open?vault=my-vault&file=file-name" ; URI to open
          ::model|text "Abc def ghi jkl mno. Abc def." ; The line where the searched text was found
          })

; ----------------------------------- View ----------------------------------- ;

(defn view [{result-map ::view-prop|result}]
  {:fx/type :v-box
   :style {:-fx-border-color "#aaaaaa"
           :-fx-border-width 1}
   :children
   [{:fx/type :label
     :v-box/margin {:left 16 :right 16 :top 16 :bottom 4}
     :text (result-map ::model|name)}
    {:fx/type :label
     :v-box/margin {:left 16 :right 16 :top 4 :bottom 4}
     :text (result-map ::model|path)}
    {:fx/type :hyperlink
     :v-box/margin {:left 16 :right 16 :top 4 :bottom 4}
     :text (result-map ::model|link)
     :on-action
     {::runtime/event-type ::event-type|link-clicked
      ::event-arg (result-map ::model|link)}}
    {:fx/type :label
     :style {:-fx-border-color "#aaaaaa"
             :-fx-border-width 1}
     :v-box/margin {:left 16 :right 16 :top 4 :bottom 16}
     :padding 8
     :max-height 100
     :pref-width Double/MAX_VALUE
     :wrap-text true
     :text (result-map ::model|text)}]})

; ---------------------------------- Update ---------------------------------- ;

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

(defn- match-json-map->result-map [kb-path-str json-deserialized]
  (let [type-str (some-> json-deserialized :type)
        data-map (some-> json-deserialized :data)
        path-map (some-> data-map :path)
        path-str (some-> path-map :text)
        path-wrapped-str path-str
        file-name-str (when path-str (last (str/split path-str #"[/\\]")))
        name-str (when file-name-str (str/replace file-name-str #"\.md$" ""))
        lines-map (some-> data-map :lines)
        md-str (some-> lines-map :text str/trim)]
    (when (= type-str "match")
      {::model|id (hash (str name-str path-str md-str))
       ::model|name name-str
       ::model|path path-wrapped-str
       ::model|link (path->uri kb-path-str path-str)
       ::model|text md-str})))

(defn- match->result [kb-path-str match-json-str]

  (let [match-json-map (json/read-str match-json-str :key-fn keyword)
        result-map (match-json-map->result-map kb-path-str match-json-map)]

    result-map))

(defmethod runtime/upset ::event-type|match-received
  [{[kb-path-str match-json-str on-new-result] ::runtime/event-args}]

  (let [new-result-map (match->result kb-path-str match-json-str)]
    {:dispatch (assoc on-new-result ::runtime/event-arg new-result-map)}))