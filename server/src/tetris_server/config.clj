(ns tetris-server.config
  (:require [aero.core :refer [read-config]]
            [clojure.java.io :as io]))

(defn load-config
  ([] (load-config :default))
  ([profile]
   (read-config (io/resource "config.edn") {:profile profile})))
