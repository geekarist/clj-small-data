(ns clj-small-data.core
  (:require [cljfx.api :as fx]
            [clj-small-data.finder :as finder]))

(def state-atom
  (atom finder/init))

(defn- reload-state! []
  (swap!
   state-atom
   (fn [_state-val]
     finder/init)))

(defn- dispatch! [[msg-key msg-val :as _message-vec]]
  (let [update-result-vec (finder/update @state-atom msg-key msg-val)
        [new-state-hash new-effect-vec] update-result-vec
        get-new-state-hash (fn [_current-state-hash] new-state-hash)]
    (swap! state-atom get-new-state-hash)
    (finder/effect! new-effect-vec dispatch!)))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc
                (fn [state-val]
                  {:fx/type finder/view :state state-val :dispatch dispatch!}))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defonce _mounted-renderer
  (fx/mount-renderer state-atom renderer))

(comment
  (println @state-atom)
  (reload-state!))