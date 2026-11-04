(ns tetris-server.db.user
  (:require
    [next.jdbc.sql :as sql])
  (:import java.sql.Timestamp
           java.time.Instant))

(defn get-user [ds m]
  (first (sql/find-by-keys ds :users m)))

(defn get-user-by-uids [ds uids]
  (sql/query ds ["select * from users where id = any(?)"
                 (long-array uids)]))

(defn save-user! [ds user]
  (sql/insert! ds :users user))

(defn update-user! [ds user]
  (sql/update! ds :users (assoc (select-keys user [:password])
                                :updated-at (Timestamp/from (Instant/now)))
               {:id (:id user)}))
