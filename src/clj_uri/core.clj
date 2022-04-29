(ns clj-uri.core
  (:import (java.io File))
  (:require [taoensso.timbre :as timbre]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn open! [uri]
  (timbre/debug "Opening URI:" uri)
  (try (let [quoted-uri-str (str "\"" uri "\"")
             ps-cmd-coll ["Remove-Item Env:\\ELECTRON_RUN_AS_NODE"
                          (format "Start-Process %s" quoted-uri-str)]
             ps-cmd-str (str/join "\n" ps-cmd-coll)
             ps-script-file (File/createTempFile "clj-uri-open" ".ps1")
             ps-script-path-str (-> ps-script-file (.getPath))
             _ (spit ps-script-file ps-cmd-str)
             cmd-resp (shell/sh "powershell" ps-script-path-str)
             ps-script-deleted? (.delete ps-script-file)]
         (timbre/debug "PowerShell script path" (-> ps-script-file (.getPath)))
         (timbre/debug "Command response:" cmd-resp)
         (timbre/debug "Script deleted?" ps-script-deleted?))
       (catch Exception e
         (timbre/debug "Error:" e))))

(comment
  (open! "obsidian://open?vault=PERSO-KB&file=202204060805.%20M%C3%A9nage")
  (open! "obsidian://open?vault=PERSO-KB&file=Index")
  (open! "https://google.fr/search?q=hello+world")
  (open! "file:///c:/program%20files")
  (open! "mailto:cpele@molotov.tv")
  (open! "mailto:cpele+test2@molotov.tv")
  (comment))