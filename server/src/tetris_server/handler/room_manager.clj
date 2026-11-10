(ns tetris-server.handler.room-manager
  (:require [tetris-server.handler.room :as room :refer [CreateRoom Room]]))

(defn put-room [rooms room] (assoc rooms (:id room) room))

(defn del-room [rooms room]
  (dissoc rooms (:id room)))

(defn new-room-manager [] (atom {:rooms {}}))

(defn get-rooms [manager]
  (vec (vals (:rooms @manager))))

(defn get-match-room [manager _uid]
  (->> (vals (:rooms @manager))
       (sort (fn [room] (if (= (:state room) :ready) 1 2)))
       (filter (fn [room] (< (count (:players room)) (:players-max room))))
       (first)))

(defn get-room [manager room-id]
  (get (:rooms @manager) room-id))

(defn add-room!
  {:malli/schema [:=> [:cat some? CreateRoom] Room]}
  [manager room]
  (let [id (format "%04x" (+ 10000 (count (keys (:rooms @manager)))))
        room (room/make-room 
               (assoc room :id id :created-at (System/currentTimeMillis)))]
    (swap! manager update :rooms put-room room)
    room))

(defn join-room! [manager room-id uid]
  (when-let [room (get-room manager room-id)]
    (let [room (room/join room uid)]
      (swap! manager update :rooms put-room room)
      room)))

(defn leave-room! [manager room-id uid]
  (when-let [room (get-room manager room-id)]
    (let [room (room/leave room uid)]
      (swap! manager update :rooms 
             (if (seq (:players room)) put-room del-room)
             room)
      room)))

(defn toggle-ready! [manager room-id uid]
  (when-let [room (get-room manager room-id)]
    (let [room (room/toggle-ready room uid)]
      (swap! manager update :rooms put-room room)
      room)))

(defn change-slot! [manager room-id uid slot]
  (when-let [room (get-room manager room-id)]
    (let [room (room/change-slot room uid slot)]
      (swap! manager update :rooms put-room room)
      room)))

(defn change-settings! [manager room-id settings]
  (when-let [room (get-room manager room-id)]
    (let [room (room/change-settings room settings)]
      (swap! manager update :rooms put-room room)
      room)))

(defn enter-game! [manager room-id force?]
  (when-let [room (get-room manager room-id)]
    (let [room (room/enter-game room force?)]
      (swap! manager update :rooms put-room room)
      room)))

(defn quit-game! [manager room-id uid]
  (when-let [room (get-room manager room-id)]
    (let [room (room/quit-game room uid)]
      (swap! manager update :rooms put-room room)
      room)))

(defn request-start-game! [manager room-id uid game-config]
  (when-let [room (get-room manager room-id)]
    (let [room (room/request-start-game room uid game-config)]
      (swap! manager update :rooms put-room room)
      room)))

(defn get-joined-room [manager uid]
  (first (filter #(room/joined? % uid) (vals (:rooms @manager)))))
