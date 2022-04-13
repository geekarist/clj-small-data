(ns clj-small-data.core
  (:require [cljfx.api :as fx]
            [clj-small-data.finder :as finder]))

(def state-atom
  (atom finder/init))

(defn- dispatch! [[msg-key msg-val :as _message-vec]]
  (fx/on-fx-thread
   (let [update-result-vec (finder/update @state-atom msg-key msg-val)
         [new-state-hash new-effect-vec] update-result-vec
         get-new-state-hash (fn [_current-state-hash] new-state-hash)]
     (swap! state-atom get-new-state-hash)
     (finder/effect! new-effect-vec dispatch!))))

(def renderer
  (fx/create-renderer
   :middleware
   (fx/wrap-map-desc
    (fn [state-val]
      {:fx/type finder/view :state state-val :dispatch dispatch!}))))

(defn -main []
  (fx/mount-renderer state-atom renderer)
  (dispatch! [:evt/raise-requested]))

(-main)