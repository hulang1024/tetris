(ns user 
  (:require
    [clj-reload.core :refer [reload]]
    [malli.dev :as dev]
    [clj-commons.pretty.repl :as repl]
    [tetris-server.server :as server]))

(println "Dev mode started.")

(repl/install-pretty-exceptions)

(dev/start!)

(defonce _server (server/start true))
