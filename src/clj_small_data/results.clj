(ns clj-small-data.results
  (:require [clj-small-data.runtime :as runtime]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:import (java.io File)))

(def init
  {::mdl:results []})

(defn view [state-map]
  {:fx/type :scroll-pane
   :fit-to-width true
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
              {::runtime/evt-type ::evt-type:link-clicked
               ::evt-arg (result-map ::mdl:link)}}
             {:fx/type :label
              :v-box/margin {:left 16 :right 16 :top 4 :bottom 16}
              :text (result-map ::mdl:text)}]})
         (state-map ::mdl:results))}})

(defn- char-wrap
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

(defn word-wrap
  ([text width]
   (let [_ (println "Split words...")
         word-coll (str/split text #"\s")
         _ (println (map #(format "'%s'" %) word-coll))
         wrapped-str (word-wrap text width word-coll)]
     wrapped-str))
  ([text width word-coll]
   (println "Word wrap: to do")))

(comment
  (word-wrap
   (str "abc def ghi jkl\n"
        "mno pqr stu vw\n")
   5))

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
        path-wrapped-str (char-wrap path-str 80)
        file-name-str (when path-str (last (str/split path-str #"[/\\]")))
        name-str (when file-name-str (str/replace file-name-str #"\.md$" ""))
        lines-map (some-> data-map :lines)
        text-str (some-> lines-map
                         :text
                         (char-wrap 80))]
    (when (= type-str "match")
      {::mdl:name name-str
       ::mdl:path path-wrapped-str
       ::mdl:link (path->uri kb-path-str path-str)
       ::mdl:line-number 123
       ::mdl:text text-str})))

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