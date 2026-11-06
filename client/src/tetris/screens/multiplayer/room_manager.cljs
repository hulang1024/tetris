(ns tetris.screens.multiplayer.room-manager 
  (:require
    [tetris.online.api :as api]
    [tetris.online.ws-client :as ws-client]))

(defn- player-info [{:keys [users]} player]
  (when player
    (let [user (first (filter #(= (:id %) (:id player)) users))]
      (merge user (update player :state keyword)))))

(defn slot-players [{:keys [room] :as manager}]
  (mapv (fn [slot]
          (let [player (first (filter #(= (:slot %) slot) (:players room)))]
            (player-info manager player)))
        (range (:players-max room))))

(defn room-stats [room]
  (let [joined-count (count (filter some? (:players room)))
        readied-count (count (filter #(= (keyword (:state %)) :readied) (:players room)))]
    {:joined-count joined-count
     :readied-count readied-count
     :all-readied? (= joined-count readied-count)}))

(defn make [room users] {:room room :users users})

(defn listen-events [state {:keys [on-player-joined on-player-left on-player-ready]}]
  (ws-client/subscribe!
    :player-joined
    (fn [{:keys [room player user]}]
      (swap! state (fn [state]
                     (-> (assoc state :room room)
                         (update :users conj user))))
      (on-player-joined (:room @state) (player-info @state player))))

  (ws-client/subscribe!
    :player-left
    (fn [{:keys [room player]}]
      (swap! state assoc :room room)
      (on-player-left (:room @state) (player-info @state player))))

  (ws-client/subscribe!
    :player-ready
    (fn [{:keys [room player]}]
      (swap! state assoc :room room)
      (on-player-ready (:room @state) (player-info @state player))))

  (ws-client/subscribe!
    :player-slot-changed
    (fn [{:keys [room]}] (swap! state assoc :room room)))
  state)

(defn leave-room []
  (api/post "/room/leave-room"))

(defn toggle-ready []
  (api/post "/room/toggle-ready"))

(defn change-slot [slot]
  (api/post "/room/change-slot" {:slot slot}))

(defn enter-game []
  (api/post "/room/enter-game"))

(defn create-room [room]
  (api/post "/room/create-room" room))

(defn join-room [room-id quick-join?]
  (api/post "/room/join-room"
            (if quick-join? {:quick_join true} {:room_id room-id})))

(defn exit []
  (doseq [event-type [:player-joined :player-left :player-ready]]
    (ws-client/unsubscribe! event-type))
  (leave-room))

