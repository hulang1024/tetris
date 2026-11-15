(ns tetris.online.user-api 
  (:require
    [tetris.online.api :refer [post get!]]))

(defn register [username password]
  (post "/register" {:username username :password password}))

(defn get-online-stats []
  (get! "/online-stats" nil))

