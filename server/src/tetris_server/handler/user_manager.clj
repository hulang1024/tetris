(ns tetris-server.handler.user-manager 
  (:require
    [tetris-server.db.user :as user-db]))

(defn safe-user-info [user]
  (dissoc user :password))

(defn get-user-info [ds m]
  (safe-user-info (user-db/get-user ds m)))

(defn get-user-infos [ds uids]
  (mapv safe-user-info (user-db/get-user-by-uids ds uids)))
