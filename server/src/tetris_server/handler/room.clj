(ns tetris-server.handler.room
  (:require [malli.util :as mu]))

(def Player
  [:map
   [:id :int]
   [:state [:enum :not-ready :readied :inprogress]]
   [:role [:enum :player :spectator]]
   [:game-config [:map]]])

(def RoomId :string)

(def Room
  [:map
   [:id RoomId]
   [:host :int]
   [:players-min [:int {:min 2}]]
   [:players-max [:int {:min 2}]]
   [:players [:vector Player]]
   [:game-settings [:map]]
   [:state [:enum :ready :inprogress]]])

(def CreateRoom
  (mu/select-keys Room
                  [:id
                   :host
                   :players-min
                   :players-max
                   :game-settings]))

(defn- find-empty-slot [room]
  (let [used-slots (set (map :slot (:players room)))]
    (first (filter #(not (contains? used-slots %))
                   (range 0 (:players-max room))))))

(defn- make-player [uid room]
  {:id uid :state
   :not-ready
   :slot (find-empty-slot room)
   :role :player})

(defn make-room
  {:malli/schema [:=> [:cat CreateRoom] Room]}
  [room]
  (assoc room
         :players [(make-player (:host room) room)]
         :state :ready))

(defn player-uids [room]
  (mapv :id (:players room)))

(defn find-player [room uid]
  (first (filter #(= (:id %) uid) (:players room))))

(defn update-player [room uid update-fn]
  (update room :players
          (fn [players]
            (let [players
                  (map (fn [player]
                         (if (= (:id player) uid)
                           (update-fn player)
                           player))
                       players)]
              (vec (sort-by :slot players))))))

(defn every-players? [room pred-state]
  (every? #(= (:state %) pred-state) (:players room)))

(defn joined? [room uid]
  (some (comp #{uid} :id) (:players room)))

(defn join [room uid]
  (if (and (< (count (:players room)) (:players-max room))
           (not (joined? room uid)))
    (update room :players conj (make-player uid room))
    room))

(defn change-host [room host]
  (assoc room :host host))

(defn change-slot [room uid slot]
  (if (not-any? #(= (:slot %) slot) (:players room))
    (update-player room uid #(assoc % :slot slot))
    room))

(defn change-role [room uid role]
  (update-player room uid #(assoc % :role role)))

(defn change-settings [room settings]
  (merge room settings))

(defn leave [room uid]
  (let [room (update room :players
                     (fn [players]
                       (filterv #(not= (:id %) uid) players)))]
    (if (seq (:players room))
      (if (= uid (:host room))
        (change-host room (-> room :players first :id))
        room)
      room)))

(defn toggle-ready [room uid]
  (update-player room uid
                 #(assoc % :state
                         (if (= (:state %) :not-ready) :readied :not-ready))))

(defn enter-game [room force?]
  (if (and (>= (count (:players room)) (:players-min room))
           (seq (filter #(= (:role %) :player) (:players room)))
           (or force? (every-players? room :readied)))
    (assoc room :state :inprogress)
    room))

(defn quit-game [room uid]
  (let [room (update-player room uid #(assoc % :state :not-ready))]
    (if (every-players? room :not-ready)
      (assoc room :state :ready)
      room)))

(defn request-start-game [room uid game-config]
  (update-player room uid #(assoc % :state :inprogress :game-config game-config)))
