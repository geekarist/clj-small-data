(ns clj-uri.core
  (:import (java.io File))
  (:require [taoensso.timbre :as timbre]
            [clojure.java.shell :as shell]))

(defn open! [uri]
  (timbre/debug "Opening URI:" uri)
  (try (let [quoted-uri-str (str "\"" uri "\"")
             ps-cmd-str (format "Start-Process %s" quoted-uri-str)
             ps-script-file (File/createTempFile "clj-uri-open" ".ps1")
             ps-script-path-str (-> ps-script-file (.getPath))
             _ (spit ps-script-file ps-cmd-str)
             cmd-resp (shell/sh "powershell.exe" "-Command" ps-script-path-str)]
         (timbre/debug "PowerShell script-path" (-> ps-script-file (.getPath)))
         (timbre/debug "Command response:" cmd-resp))
       (catch Exception e
         (timbre/debug "Error:" e))))

(comment
  (open! "obsidian://open?vault=PERSO-KB&file=202204241417.%20Cr%C3%A9dit%20travaux")
  (open! "obsidian://open?vault=PERSO-KB&file=202204060805.%20M%C3%A9nage")
  (open! "obsidian://open?vault=PERSO-KB&file=Index")
  (open! "https://google.fr/search?q=hello+world")
  (open! "file:///c:/program%20files")
  (open! "mailto:cpele@molotov.tv")
  (open! "mailto:cpele+test2@molotov.tv")
  (shell/sh "powershell" "C:\\Users\\chris\\AppData\\Local\\Temp\\clj-uri-open10673641866898109012.ps1")
  (comment))