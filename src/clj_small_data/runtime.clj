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
    (let [sh-cmd-vec (arg-map ::effect|sh|cmd)
          _ (println "Command vector:" sh-cmd-vec)
          cmd-out-map (apply shell/sh sh-cmd-vec)
          _ (println "Command output:" cmd-out-map)
          cmd-std-out-str (cmd-out-map :out)
          got-output-evt (arg-map ::effect|sh|on-command-output)
          evt-map (assoc got-output-evt
                         ::effect|sh|cmd-out cmd-std-out-str)]
      (dispatch! evt-map))))

(defn effects [context-atom]
  {::effect|log #(log! %1 %2)
   ::effect|state #(set-state! context-atom %1 %2)
   ::effect|sh #(sh! %1 %2)
   ::effect|open-uri (fn [uri _dispatch!] (curi/open! uri))
   ::effect|dispatch (fn [arg dispatch!] (dispatch! arg))})

(defn coeffects [context-atom]
  {::coeffect|state #(fx/sub-val (deref context-atom) identity)})

(defmulti upset ::event-type)

(defn create! [init get-view-fn upset]
  (let [cache-factory cache/lru-cache-factory
        context (fx/create-context init cache-factory)
        context-atom (atom context)]
    (fx/create-app context-atom
                   :event-handler upset
                   :co-effects (coeffects context-atom)
                   :effects (effects context-atom)
                   :desc-fn (fn [_]
                              {:fx/type (get-view-fn)}))))

(defn apply-changes! [app]
  (let [renderer (app :renderer)]
    (renderer)))

(defn view-ctx [view]
  (fn  [{context-obj :fx/context}]
    (let [get-state #(fx/sub-val context-obj %)]
      (view get-state))))


