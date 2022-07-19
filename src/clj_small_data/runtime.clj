(ns clj-small-data.runtime
  (:require [clj-uri.core :as curi]
            [cljfx.api :as fx :refer [create-renderer
                                      fn->lifecycle-with-context keyword->lifecycle mount-renderer
                                      wrap-map-desc]]
            [cljfx.defaults :as defaults]
            [cljfx.event-handler :as event-handler]
            [cljfx.lifecycle :refer [wrap-context-desc]]
            [cljfx.renderer :as renderer]
            [clojure.core.cache :as cache]
            [clojure.java.shell :as shell]))

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

(defn- dispatch-many! [events-coll dispatch!]
  (doseq [event events-coll]
    (dispatch! event)))

(defn effects [_context-atom]
  {::effect|log #(log! %1 %2)
   ::effect|sh #(sh! %1 %2)
   ::effect|open-uri (fn [uri _dispatch!] (curi/open! uri))
   ::effect|dispatch (fn [arg dispatch!] (dispatch! arg))
   ::effect|dispatches #(dispatch-many! %1 %2)})

(defn coeffects [_context-atom]
  {})

(defmulti upset ::event-type)

(defn create-app-without-mount
  "Create an app exactly like cljfx.api/create-app, but don't mount the renderer"
  [*context & {:keys [event-handler
                      desc-fn
                      co-effects
                      effects
                      async-agent-options
                      renderer-middleware
                      renderer-error-handler]
               :or {co-effects {}
                    effects {}
                    async-agent-options {}
                    renderer-middleware identity
                    renderer-error-handler renderer/default-error-handler}}]
  (let [handler (-> event-handler
                    (event-handler/wrap-co-effects
                     (defaults/fill-co-effects co-effects *context))
                    (event-handler/wrap-effects
                     (defaults/fill-effects effects *context))
                    (event-handler/wrap-async
                     (defaults/fill-async-handler-options async-agent-options)))
        renderer (create-renderer
                  :error-handler renderer-error-handler
                  :middleware (comp
                               wrap-context-desc
                               (wrap-map-desc desc-fn)
                               renderer-middleware)
                  :opts {:fx.opt/map-event-handler handler
                         :fx.opt/type->lifecycle #(or (keyword->lifecycle %)
                                                      (fn->lifecycle-with-context %))})]
    {:renderer renderer
     :handler handler}))

(defn create! [init-event-map get-view-fn upset]
  (let [cache-factory cache/lru-cache-factory
        context (fx/create-context {} cache-factory)
        context-atom (atom context)
        app (create-app-without-mount context-atom
                                      :event-handler upset
                                      :co-effects (coeffects context-atom)
                                      :effects (effects context-atom)
                                      :desc-fn (fn [_]
                                                 {:fx/type (get-view-fn)}))
        {handler :handler renderer :renderer} app
        _ (handler init-event-map)
        _ (mount-renderer context-atom renderer)]
    app))

(defn apply-changes! [app]
  (let [renderer (app :renderer)]
    (renderer)))