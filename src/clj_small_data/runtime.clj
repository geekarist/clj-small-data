(ns clj-small-data.runtime
  (:require [cljfx.api :as fx]
            [clojure.java.shell :as shell]
            [clj-uri.core :as curi]
            [clojure.core.cache :as cache]))

(defn- log! [arg _dispatch!]
  (println arg))

(defn- sh! [arg-map dispatch!]
  (future
    (let [sh-cmd-vec (arg-map ::effect|sh|cmd)
          _ (println "Command vector:" sh-cmd-vec)
          cmd-out-map (apply shell/sh sh-cmd-vec)
          ;; _ (println "Command output:" cmd-out-map)
          cmd-std-out-str (cmd-out-map :out)
          got-output-evt (arg-map ::effect|sh|on-command-output)
          evt-map (assoc got-output-evt
                         ::effect|sh|cmd-out cmd-std-out-str)]
      (dispatch! evt-map))))

(defn effects [_context-atom]
  {::effect|log #(log! %1 %2)
   ::effect|sh #(sh! %1 %2)
   ::effect|open-uri (fn [uri _dispatch!] (curi/open! uri))
   ::effect|dispatch (fn [arg dispatch!] (dispatch! arg))})

(defn coeffects [_context-atom]
  {})

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