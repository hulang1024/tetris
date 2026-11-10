(ns tetris.online.room-api 
  (:require
    [tetris.online.api :as api]))

(defn leave-room []
  (api/post "/room/leave-room"))

(defn toggle-ready []
  (api/post "/room/toggle-ready"))

(defn change-slot [slot]
  (api/post "/room/change-slot" {:slot slot}))

(defn update-room-settings [settings]
  (api/post "/room/change-settings" settings))

(defn enter-game [force?]
  (api/post "/room/enter-game" {:force-start force?}))

(defn quit-game []
  (api/post "/room/quit-game"))

(defn request-start-game [game-config]
  (api/post "/room/request-start-game" game-config))

(defn create-room [room]
  (api/post "/room/create-room" room))

(defn join-room [room-id & [quick-join?]]
  (api/post "/room/join-room"
            (if quick-join? {:quick_join true} {:room_id room-id})))
