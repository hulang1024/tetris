(ns tetris.screens.multiplayer.room-manager 
  (:require [tetris.online.ws-client :as ws-client]))

(defn player-info [player users]
  (when player
    (let [user (first (filter #(= (:id %) (:id player)) users))]
      (merge user (-> player
                      (update :state keyword)
                      (update :role keyword))))))

(defn local-player [{:keys [room users]} local-uid]
  (player-info (first (filter #(= local-uid (:id %)) (:players room))) users))

(defn slot-players [{:keys [room] :as manager}]
  (mapv (fn [slot]
          (let [player (first (filter #(= (:slot %) slot) (:players room)))]
            (player-info player (:users manager))))
        (range (:players-max room))))

(defn room-stats [room]
  (let [joined-count (count (filter some? (:players room)))
        readied-count (count (filter #(= (keyword (:state %)) :readied) (:players room)))]
    {:joined-count joined-count
     :readied-count readied-count
     :all-readied? (= joined-count readied-count)}))

(defn ->room [room]
  (-> room
      (update :state keyword)
      (update-in [:game-settings :mode] keyword)
      (update-in [:game-settings :rs] keyword)))

(defn make [room users] {:room (->room room) :users users})

(defn- update-room! [state room]
  (swap! state assoc :room (->room room)))

(defn listen-events [state {:keys [on-player-joined on-player-left on-player-ready on-game-entered]}]
  (ws-client/subscribe!
    :room :player-joined
    (fn [{:keys [room player user]}]
      (update-room! state room)
      (swap! state update :users conj user)
      (on-player-joined (:room @state) (player-info player (:users @state))))
    true)

  (ws-client/subscribe!
    :room :player-left
    (fn [{:keys [room player]}]
      (update-room! state room)
      (on-player-left (:room @state) (player-info player(:users @state)))))

  (ws-client/subscribe!
    :room :player-ready
    (fn [{:keys [room player]}]
      (update-room! state room)
      (on-player-ready (:room @state) (player-info player (:users @state)))))

  (ws-client/subscribe!
    :room :player-changed
    (fn [{:keys [room]}] (update-room! state room)))

  (ws-client/subscribe!
    :room :player-game-quitted
    (fn [{:keys [room]}] (update-room! state room)))

  (ws-client/subscribe!
    :room :room-changed
    (fn [{:keys [room]}] (update-room! state room)))

  (ws-client/subscribe!
    :room :game-entered
    (fn [{:keys [room]}]
      (update-room! state room)
      (on-game-entered (:room @state))))
  state)

(defn unlisten-events []
  (ws-client/unsubscribe! :room :all))
