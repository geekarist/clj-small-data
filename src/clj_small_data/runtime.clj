(ns clj-small-data.runtime
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]))

(defn- set-state! [context-atom state-map _dispatch!]
  (swap! context-atom fx/reset-context state-map))

(defn- log! [arg _dispatch!]
  (println arg))

(defn view-context [{:keys [fx/context]} view-fn]
  (let [state-map (fx/sub-val context identity)]
    (view-fn state-map)))

(defn create! [init get-view-fn upset coeffects effects]
  (let [cache-factory cache/lru-cache-factory
        context (fx/create-context init cache-factory)
        context-atom (atom context)]
    (fx/create-app context-atom
                   :event-handler upset
                   :co-effects
                   (assoc coeffects
                          ::coe-state #(fx/sub-val (deref context-atom) identity))
                   :effects
                   (assoc effects
                          ::eff-log #(log! %1 %2)
                          ::eff-state #(set-state! context-atom %1 %2))
                   :desc-fn (fn [_]
                              {:fx/type #(view-context % (get-view-fn))}))))

(defn apply-changes! [app]
  (let [renderer (app :renderer)]
    (renderer)))