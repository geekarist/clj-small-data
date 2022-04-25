(ns clj-uri.core
  (:require [taoensso.timbre :as timbre])
  (:import (com.profesorfalken.jpowershell PowerShell)))

(defn open! [uri]
  (timbre/debug "Opening URI:" uri)
  (try (let [quoted-uri-str (str "\"" uri "\"")
             cmd-str (timbre/spy (format "Start-Process %s" quoted-uri-str))
             cmd-resp (PowerShell/executeSingleCommand cmd-str)
             cmd-err? (.isError cmd-resp)
             cmd-output-str (.getCommandOutput cmd-resp)]
         (timbre/debug "Error?" cmd-err?)
         (timbre/debug "Command output:" cmd-output-str))
       (catch Exception e
         (timbre/debug "Error:" e))))

