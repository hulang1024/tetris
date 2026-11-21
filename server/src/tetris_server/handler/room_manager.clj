(ns tetris-server.handler.room-manager
  (:require [tetris-server.handler.room :as room :refer [CreateRoom Room]]))

(defn new-room-manager [] (atom {:last-room-id nil :rooms {}}))

(defn get-room [manager room-id]
  (get-in @manager [:rooms room-id]))

(defn get-rooms [manager]
  (vec (vals (:rooms @manager))))

(defn get-match-room [manager _uid]
  (->> (vals (:rooms @manager))
       (filter (fn [room] (and (:public room)
                               (< (count (:players room)) (:players-max room)))))
       (sort #(count (:players %)))
       (sort #(if (= (:state %) :ready) 0 1))
       (first)))

(defn- update-room! [manager room-id update-fn & update-args]
  (swap! manager update :rooms
         (fn [rooms]
           (if-let [room (get rooms room-id)]
             (if-let [updated (apply update-fn room update-args)]
               (assoc rooms room-id updated)
               (dissoc rooms room-id))
             rooms)))
  (get-in @manager [:rooms room-id]))

(defn add-room!
  {:malli/schema [:=> [:cat some? CreateRoom] Room]}
  [manager room]
  (let [created-at (System/currentTimeMillis)]
    (swap! manager
           (fn [manager]
             (let [room-id (format "%04x" (or (some-> (:last-room-id manager)
                                                      (Integer/parseInt 16)
                                                      inc)
                                              10000))
                   created (room/make-room (assoc room :id room-id :created-at created-at))]
               (-> manager
                   (assoc-in [:rooms room-id] created)
                   (assoc :last-room-id room-id)))))
    (get-in @manager [:rooms (:last-room-id @manager)])))

(defn join-room! [manager room-id uid]
  (update-room! manager room-id room/join uid))

(defn leave-room! [manager room-id uid]
  (update-room! manager room-id
                (fn [room]
                  (let [room (room/leave room uid)]
                    (when (seq (:players room)) room)))))

(defn toggle-ready! [manager room-id uid]
  (update-room! manager room-id room/toggle-ready uid))

(defn change-slot! [manager room-id uid slot]
  (update-room! manager room-id room/change-slot uid slot))

(defn change-role! [manager room-id uid role]
  (update-room! manager room-id room/change-role uid role))

(defn change-room-settings! [manager room-id settings]
  (update-room! manager room-id room/change-settings settings))

(defn enter-game! [manager room-id force?]
  (update-room! manager room-id room/enter-game force?))

(defn quit-game! [manager room-id uid]
  (update-room! manager room-id room/quit-game uid))

(defn request-start-game! [manager room-id uid game-config]
  (update-room! manager room-id room/request-start-game uid game-config))

(defn get-joined-room [manager uid]
  (first (filter #(room/joined? % uid) (vals (:rooms @manager)))))
