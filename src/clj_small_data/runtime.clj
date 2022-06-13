(ns clj-small-data.runtime
  (:require [cljfx.api :as fx]
            [clojure.java.shell :as shell]
            [clj-uri.core :as curi]
            [clojure.core.cache :as cache]))

(defn- set-state! [context-atom state-map _dispatch!]
  (swap! context-atom fx/reset-context state-map))

(defn- log! [arg _dispatch!]
  (println arg))

(defn- sh! [arg-map dispatch!]
  (future
    (let [got-output-evt (arg-map ::eff:sh:got-output)
          sh-cmd-vec (arg-map ::eff:sh:cmd)
          _ (println "Command vector:" sh-cmd-vec)
          cmd-out-map (apply shell/sh sh-cmd-vec)
          _ (println "Command output:" cmd-out-map)
          cmd-std-out-str (cmd-out-map :out)
          evt-map (assoc got-output-evt
                         ::eff:sh:cmd-out cmd-std-out-str)]
      (dispatch! evt-map))))

(defn effects [context-atom]
  {::eff:log #(log! %1 %2)
   ::eff:state #(set-state! context-atom %1 %2)
   ::eff:sh #(sh! %1 %2)
   ::eff:open-uri (fn [uri _dispatch!] (curi/open! uri))
   ::eff:dispatch (fn [arg dispatch!] (dispatch! arg))})

(defn coeffects [context-atom]
  {::coe-state #(fx/sub-val (deref context-atom) identity)})

(defn view-context [{:keys [fx/context]} view-fn]
  (let [state-map (fx/sub-val context identity)]
    (view-fn state-map)))

(defmulti upset ::evt-type)

(defn create! [init get-view-fn upset]
  (let [cache-factory cache/lru-cache-factory
        context (fx/create-context init cache-factory)
        context-atom (atom context)]
    (fx/create-app context-atom
                   :event-handler upset
                   :co-effects (coeffects context-atom)
                   :effects (effects context-atom)
                   :desc-fn (fn [_]
                              {:fx/type #(view-context % (get-view-fn))}))))

(defn apply-changes! [app]
  (let [renderer (app :renderer)]
    (renderer)))