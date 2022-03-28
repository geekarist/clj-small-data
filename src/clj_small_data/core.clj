(ns clj-small-data.core
  (:require [cljfx.api :as fx]))

(defn root [{:keys [showing]}]
  {:fx/type :stage
   :showing showing
   :title "Hello"
   :width 300
   :height 100
   :scene
   {:fx/type :scene
    :root
    {:fx/type :v-box
     :alignment :top-left
     :children
     [{:fx/type :text-field
       :v-box/margin {:bottom 8 :left 8 :right 8}
       :text "Hello again 🙂"}
      {:fx/type :label
       :v-box/margin {:bottom 8 :left 8 :right 8}
       :text "First result"}
      {:fx/type :label
       :v-box/margin {:bottom 8 :left 8 :right 8}
       :text "Second result"}
      {:fx/type :label
       :v-box/margin {:left 8 :right 8}
       :text "Third result"}]}}})

(comment
  (let [renderer (fx/create-renderer)]
    (renderer {:fx/type root :showing true})))
