(ns clj-small-data.findings
  (:require [clj-small-data.result :as result]
            [clj-small-data.runtime :as runtime]
            [cljfx.api :as fx]
            [clojure.string :as str]))

(defn- view-results-count [{results-coll ::view-prop|results}]
  {:fx/type :label
   :text-alignment :center
   :padding {:bottom 16 :left 16 :right 16}
   :max-width Double/MAX_VALUE
   :text (format "Found %d results." (count results-coll))})

(defn- desc-one-result [result-map]
  {:fx/type result/view
   :fx/key (result-map ::model|id)
   :v-box/margin {:left 16 :right 16 :bottom 16}
   ::result/view-prop|result result-map})

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

(defmethod runtime/upset ::event-type|init
  [{context :fx/context
    [on-receive-results] ::event-args}]
  {:context (fx/swap-context context assoc
                             ::model|on-receive-results on-receive-results
                             ::model|results [])})

(defmethod runtime/upset ::event-type|search-output-received
  [{cmd-out-str ::runtime/effect|sh|cmd-out
    kb-path-str ::event-arg|kb-path}]

  (let [split-output-strs-coll (str/split-lines cmd-out-str)
        match->result #(result/from-match kb-path-str %)
        all-new-results-coll (map match->result split-output-strs-coll)
        some-new-results-coll (filter some? all-new-results-coll)]

    {::runtime/effect|dispatch
     {::runtime/event-type ::event-type|results-received
      ::event-arg|new-results some-new-results-coll}}))

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
          old-results-coll (fx/sub-val context ::model|results)
          result-batch-coll (->> additional-results-coll
                                 (take batch-size-num)
                                 (concat old-results-coll))]

      {:context
       (fx/swap-context context assoc ::model|results result-batch-coll)

       ::runtime/effect|dispatch
       {::runtime/event-type ::event-type|additional-results-received
        ::event-arg|additional-results (drop batch-size-num additional-results-coll)}})

    {::runtime/effect|dispatch (fx/sub-val context ::model|on-receive-results)}))

