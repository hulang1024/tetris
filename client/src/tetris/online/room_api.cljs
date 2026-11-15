(ns tetris.online.room-api 
  (:require
    [tetris.online.api :refer [post]]))

(defn leave-room []
  (post "/room/leave-room"))

(defn toggle-ready []
  (post "/room/toggle-ready"))

(defn change-slot [slot]
  (post "/room/change-slot" {:slot slot}))

(defn change-role [role]
  (post "/room/change-role" {:role role}))

(defn change-room-settings [settings]
  (post "/room/change-room-settings" settings))

(defn enter-game [force?]
  (post "/room/enter-game" {:force-start force?}))

(defn quit-game []
  (post "/room/quit-game"))

(defn request-start-game [game-config]
  (post "/room/request-start-game" game-config))

(defn create-room [room]
  (post "/room/create-room" room))

(defn join-room [room-id & [quick-join?]]
  (post "/room/join-room" (if quick-join? {:quick_join true} {:room_id room-id})))
