(ns tetris-server.handler.room-manager
  (:require [tetris-server.handler.room :as room :refer [Room RoomId]]))

(def Rooms [:map-of RoomId Room])

(defn put-room [rooms room] (assoc rooms (:id room) room))

(defn del-room [rooms room]
  (dissoc rooms (:id room)))

(defn new-room-manager [] {:rooms (atom {})})

(defn get-rooms [rm]
  (vec (vals @(:rooms rm))))

(defn get-room [rm room-id]
  (get @(:rooms rm) room-id))

(defn add-room!
  {:malli/schema [:=> [:cat Rooms Room] Rooms]}
  [rm room]
  (let [room (room/make-room 
               (assoc room
                      :id (str (java.util.UUID/randomUUID))
                      :created-at (System/currentTimeMillis)))]
    (swap! (:rooms rm) put-room room)
    room))

(defn join-room! [rm room-id uid]
  (when-let [room (get-room rm room-id)]
    (let [room (room/join room uid)]
      (swap! (:rooms rm) put-room room)
      room)))

(defn leave-room! [rm room-id uid]
  (when-let [room (get-room rm room-id)]
    (let [room (room/leave room uid)]
      (swap! (:rooms rm)
             (if (seq (:players room)) put-room del-room)
             room)
      room)))

(defn toggle-ready! [rm room-id uid]
  (when-let [room (get-room rm room-id)]
    (let [room (room/toggle-ready room uid)]
      (swap! (:rooms rm) put-room room)
      room)))

(defn enter-game! [rm room-id force?]
  (when-let [room (get-room rm room-id)]
    (let [room (room/enter-game room force?)]
      (swap! (:rooms rm) put-room room)
      room)))

(defn request-start-game! [rm room-id uid]
  (when-let [room (get-room rm room-id)]
    (let [room (room/request-start-game room uid)]
      (swap! (:rooms rm) put-room room)
      room)))

(defn get-joined-room [rm uid]
  (first (filter #(room/joined? % uid) (vals @(:rooms rm)))))
