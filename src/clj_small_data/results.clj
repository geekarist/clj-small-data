(ns clj-small-data.results
  (:require [clj-small-data.runtime :as runtime]
            [cljfx.api :as fx]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:import (java.io File)))

(defn- view-one-result [{result-map ::view-prop|result}]
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

(defn- view-results-count [{results-coll ::view-prop|results}]
  {:fx/type :label
   :text-alignment :center
   :padding {:bottom 16 :left 16 :right 16}
   :max-width Double/MAX_VALUE
   :text (format "Found %d results." (count results-coll))})

(defn- desc-one-result [result-map]
  {:fx/type view-one-result
   :fx/key (result-map ::model|id)
   :v-box/margin {:left 16 :right 16 :bottom 16}
   ::view-prop|result result-map})

(defn- desc-results-count [results-coll]
  {:fx/type view-results-count
   ::view-prop|results results-coll})

(defn view-some-results [{results-coll ::view-prop|results}]
  {:fx/type :scroll-pane
   :fit-to-width true
   :content {:fx/type :v-box
             :padding {:top 16}
             :children
             (concat [(desc-results-count results-coll)]
                     (map desc-one-result results-coll))}})

(defn view-empty-results [_arg]
  {:fx/type :label
   :style {:-fx-border-color ["#aaaaaa" "#00000000" "#00000000" "#00000000"]
           :-fx-border-width 0.5}
   :max-width Double/MAX_VALUE
   :padding {:top 16 :bottom 16 :left 16 :right 16}
   :text "No search results."})

(defn view [{context :fx/context}]
  (println "Executing results view")
  (let [results-coll (fx/sub-val context ::model|results)]
    {:fx/type (if (not-empty results-coll)
                view-some-results
                view-empty-results)
     ::view-prop|results results-coll}))

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

(defn- json-map->result [kb-path-str json-deserialized]
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

(defmethod runtime/upset ::event-type|init
  [{context :fx/context
    [on-receive-results] ::event-args}]
  {:context (fx/swap-context context assoc
                             ::model|on-receive-results on-receive-results
                             ::model|results [])})

(defmethod runtime/upset ::event-type|search-output-received
  [{cmd-out-str ::runtime/effect|sh|cmd-out
    kb-path-str ::event-arg|kb-path}]

  {::runtime/effect|dispatch
   {::runtime/event-type ::event-type|search-output-trimmed
    ::event-arg|trimmed-cmd-out (str/trim-newline cmd-out-str)
    ::event-arg|kb-path kb-path-str}})

(defmethod runtime/upset ::event-type|search-output-trimmed
  [{trimmed-cmd-out-str ::event-arg|trimmed-cmd-out
    kb-path-str ::event-arg|kb-path}]
  {::runtime/effect|dispatch
   {::runtime/event-type ::event-type|search-output-comma-separated
    ::event-arg|comma-separated-cmd-out (str/replace trimmed-cmd-out-str #"\n" ",")
    ::event-arg|kb-path kb-path-str}})

(defmethod runtime/upset ::event-type|search-output-comma-separated
  [{cmd-out-str ::event-arg|comma-separated-cmd-out
    kb-path-str ::event-arg|kb-path}]

  (let [kb-path-str kb-path-str
        comma-separated-cmd-out-str cmd-out-str
        output-json-str (str "[" comma-separated-cmd-out-str "]")
        output-json-coll (json/read-str output-json-str :key-fn keyword)]
    {::runtime/effect|dispatch
     {::runtime/event-type ::event-type|search-output-deserialized
      ::event-arg|kb-path kb-path-str
      ::event-arg|deserialized-search-output output-json-coll}}))

(defmethod runtime/upset ::event-type|search-output-deserialized
  [{output-json-coll ::event-arg|deserialized-search-output
    kb-path-str ::event-arg|kb-path}]

  (let [new-results-coll
        (->> output-json-coll
             (map (partial json-map->result kb-path-str))
             (filter some?))
        event-map
        {::runtime/event-type ::event-type|results-received
         ::event-arg|new-results new-results-coll}]
    {::runtime/effect|dispatch event-map}))

(defmethod runtime/upset ::event-type|results-received
  [{new-results-coll ::event-arg|new-results
    context :fx/context}]

  {:context
   (fx/swap-context context assoc ::model|results [])

   ::runtime/effect|dispatch
   {::runtime/event-type ::event-type|additional-results-received
    ::event-arg|additional-results new-results-coll}})

(defmethod runtime/upset ::event-type|additional-results-received
  [{context :fx/context
    additional-results-coll ::event-arg|additional-results}]

  (if (not-empty additional-results-coll)

    (let [batch-size-num 100
          old-results-coll (fx/sub-val context ::model|results)]

      {:context
       (fx/swap-context context assoc ::model|results
                        (concat old-results-coll (take batch-size-num additional-results-coll)))


       ::runtime/effect|dispatch
       {::runtime/event-type ::event-type|additional-results-received
        ::event-arg|additional-results (drop batch-size-num additional-results-coll)}})

    {::runtime/effect|dispatch (fx/sub-val context ::model|on-receive-results)}))

(defmethod runtime/upset ::event-type|link-clicked
  [{:keys [::event-arg]}]
  {::runtime/effect|open-uri event-arg})