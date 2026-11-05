(ns tetris.screens.multiplayer.room-manager 
  (:require
    [tetris.online.api :as api]
    [tetris.online.ws-client :as ws-client]))

(defn- get-player-info [player users]
  (merge player (first (filter #(= (:id %) (:id player)) users))))

(defn make [room users]
  {:room (dissoc room :players)
   :players (mapv (fn [slot]
                    (let [player (first (filter #(= (:slot %) slot) (:players room)))]
                      (get-player-info player users)))
                  (range (:players-max room)))})

(defn listen-events [state {:keys [on-player-joined on-player-left on-player-ready]}]
  (ws-client/subscribe!
    :player-joined
    (fn [{{:keys [player user]} :data}]
      (swap! state assoc-in [:players (:slot player)] (get-player-info player [user]))
      (on-player-joined)))

  (ws-client/subscribe!
    :player-left
    (fn [{{:keys [room player]} :data}]
      (swap! state (fn [state]
                     (-> (assoc-in state [:players (:slot player)] nil)
                         (assoc-in [:room :host] (:host room)))))
      (on-player-left)))

  (ws-client/subscribe!
    :player-ready
    (fn [{{:keys [room player]} :data}]
      (let [ready? (= (:state player) "readied")
            me? (= (:id player) (:id (api/local-user)))
            joined-count (count (filter some? (:players room)))
            readied-count (count (filter #(= (:state %) "readied") (:players room)))
            all-readied? (= joined-count readied-count)]
        (swap! state assoc-in [:players (:slot player) :state] (:state player))
        (on-player-ready me? ready? all-readied?))))
  state)

(defn leave-room []
  (api/post "/room/leave-room"))

(defn toggle-ready []
  (api/post "/room/toggle-ready"))

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

