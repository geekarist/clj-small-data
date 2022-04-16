(ns clj-small-data.finder
  (:refer-clojure :exclude [update])
  (:require
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.data.json :as json])
  (:import (java.io File)
           (javafx.stage Window)))

(def init
  {:mdl/title "Small Data Finder"
   :mdl/iconified false
   :mdl/kb-path "C:/Users/chris/Google Drive/DriveSyncFiles/PERSO-KB"
   :mdl/search-text ""
   :mdl/search-field-placeholder "Please enter your search text"
   :mdl/results
   []})

(defn view [{state-map :state dispatch! :dispatch}]

  ;; Window
  {:fx/type :stage :showing true :title (state-map :mdl/title)
   :iconified (state-map :mdl/iconified)
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
         :h-box/hgrow :always :text (state-map :mdl/search-text)
         :prompt-text (state-map :mdl/search-field-placeholder)
         :on-text-changed #(dispatch! [:evt/change-search-query %])}

        ;; Buttons
        {:fx/type :button :text "Clear"
         :on-action (fn [_] (dispatch! [:evt/clear-btn-pressed]))}
        {:fx/type :button :text "Find" :h-box/margin {:left 8}
         :on-action (fn [_] (dispatch! [:evt/search-btn-pressed]))}
        {:fx/type :button :text "Redraw" :h-box/margin {:left 8}
         :on-action (fn [_] (dispatch! [:evt/redraw-btn-pressed]))}
        {:fx/type :button :text "Log"
         :on-action (fn [_] (dispatch! [:evt/log-btn-pressed]))}]}

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
                  :text (result-map :mdl/name)}
                 {:fx/type :label
                  :v-box/margin {:left 16 :right 16 :top 4 :bottom 4}
                  :text (result-map :mdl/path)}
                 {:fx/type :hyperlink
                  :v-box/margin {:left 16 :right 16 :top 4 :bottom 4}
                  :text (result-map :mdl/link)
                  :on-action
                  (fn [_] (dispatch! [:evt/link-clicked (result-map :mdl/link)]))}
                 {:fx/type :label
                  :v-box/margin {:left 16 :right 16 :top 4 :bottom 16}
                  :text (result-map :mdl/text)}]})
             (state-map :mdl/results))}})}}})

(defn- wrap-long-lines
  "Take `string` and insert line endings every `width` characters."
  [string width]
  (when string
    (loop [i 0
           acc ""]
      (letfn [(out-of-bounds? [string i]
                (>= i (.length string)))
              (next-index-fn [i] (inc i))
              (mult? [width i]
                (and (not= 0 i)
                     (= 0 (mod i width))))
              (next-acc-fn [string width i acc]
                (str acc
                     (nth string i)
                     (if (mult? width i) "\n" "")))]
        (if (out-of-bounds? string i)
          acc
          (recur (next-index-fn i)
                 (next-acc-fn string width i acc)))))))

(defn- path->uri [kb-path-str path-str]
  ;; kb-path-str: "c:\a\b\c-kb"
  ;; path-str: "c:\a\b\c-kb\ab cd.md"
  ;; result: "obsidian://open?vault=c-kb&file=ab%20cd.md"
  (let [vault-str (-> kb-path-str File. .getName)
        file-str (-> path-str File. .getName)
        file-no-ext-str (str/replace file-str #".md$" "")
        file-encoded-str (java.net.URLEncoder/encode file-no-ext-str "UTF-8")]
    (format "obsidian://open?vault=%s&file=%s" vault-str file-encoded-str)
    (format "https://google.com/search?q=%s+%s" vault-str file-encoded-str)))

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
      {:mdl/name name-str
       :mdl/path path-wrapped-str
       :mdl/link (path->uri kb-path-str path-str)
       :mdl/line-number 123
       :mdl/text text-str})))

(defn- new-state-on-search-output-received [state-map search-output-json-str]
  (let [split-output-json-vec (str/split search-output-json-str #"\n")
        kb-path-str (state-map :mdl/kb-path)
        mdl-item-map (map (fn [json-str] (json->result kb-path-str json-str))
                          split-output-json-vec)
        results (filter some? mdl-item-map)]
    (assoc state-map :mdl/results results)))

(defn advance [state-map event-key event-val]

  (condp = event-key

    :evt/change-search-query
    (let [new-state-hash (assoc state-map :mdl/search-text event-val)
          new-effect-vec nil]
      [new-state-hash new-effect-vec])

    :evt/redraw-btn-pressed
    (let [new-state-hash state-map
          new-effect-vec nil]
      [new-state-hash new-effect-vec])

    :evt/clear-btn-pressed
    (let [new-state-hash init
          new-effect-vec nil]
      [new-state-hash new-effect-vec])

    :evt/search-btn-pressed
    (let [new-state-hash state-map
          new-effect-vec
          [:eff/search [(state-map :mdl/kb-path) (state-map :mdl/search-text)]]]
      [new-state-hash new-effect-vec])

    :evt/search-output-received
    (let [new-state-hash
          (new-state-on-search-output-received state-map event-val)]
      [new-state-hash nil])

    :evt/log-btn-pressed
    (let [new-state-hash state-map
          new-effect-vec [:eff/log state-map]]
      [new-state-hash new-effect-vec])

    :evt/raise-requested
    (let [new-state-hash state-map
          new-effect-vec [:eff/raise-window]]
      [new-state-hash new-effect-vec])

    :evt/iconify-requested
    (let [new-state-map (assoc state-map :mdl/iconified true)
          new-effect-vec nil]
      [new-state-map new-effect-vec])

    :evt/deiconify-requested
    (let [new-state-map (assoc state-map :mdl/iconified false)
          new-effect-vec nil]
      [new-state-map new-effect-vec])

    :evt/link-clicked
    (let [new-effect-vec [:eff/open-uri event-val]]
      [state-map new-effect-vec])

    (do (println "Unknown message key:" event-key)
        [state-map nil])))

(defn- search-file! [[search-dir query] dispatch!]
  (future
    (let [result (shell/sh "rg" "--json" query search-dir)
          output (result :out)]
      (dispatch! [:evt/search-output-received output]))))

(defn- raise-window! [dispatch!]
  (dispatch! [:evt/iconify-requested])
  (future
    (dispatch! [:evt/deiconify-requested])))

(defn- open-uri! [uri]
  (-> (Window/getWindows)
      (first)
      (.getApplication)
      (.getHostServices)
      (.showDocument uri)))

(defn effect! [[key value :as _new-effect-vec] dispatch!]
  (condp = key
    :eff/search (search-file! value dispatch!)
    :eff/log (println "State:" value)
    :eff/raise-window (raise-window! dispatch!)
    :eff/open-uri (open-uri! value)
    nil nil ; Ignore `nil` effect
    (println "Effect not found:" key)))

