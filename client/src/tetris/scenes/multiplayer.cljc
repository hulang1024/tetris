(ns tetris.scenes.multiplayer 
  (:require
    [tetris.core.game :as game]
    [tetris.core.tick :as tick]
    [tetris.online.ws-client :as ws-client]
    [tetris.scenes.game :refer [make-game-options]]
    [tetris.scenes.network-replay :as network-replay]))

(defn make-players [{:keys [players local-uid game-config]}]
  (mapv
    (fn [player]
      (let [game-state (tick/initial-game
                         (make-game-options (merge game-config (:game-config player)))) ]
        (merge player
               {:game-state game-state
                :game-status :playing
                :game-view nil
                :network-replayer (when (not= local-uid (:id player))
                                    (network-replay/make-replayer (:id player)))
                :input-state nil})))
    players))

(defn update-player-games [players]
  (reduce-kv
    (fn [players uid {:keys [input-state game-state game-status] :as player}]
      (if (or (some? input-state) (= game-status :game-over))
        (let [game-state (cond-> (assoc game-state :events [])
                           (= game-status :playing) (tick/step input-state))
              game-status (if (game/find-event :game-over (:events game-state))
                            :game-over
                            game-status)]
          (assoc players uid
                 (assoc player
                        :game-state game-state
                        :game-status game-status)))
        players))
    players
    players))

(defn update-network-inputs [players local-input-state]
  (reduce-kv
    (fn [m uid {:keys [network-replayer] :as player}]
      (assoc m uid
             (if network-replayer
               (let [[input-state replayer] (network-replay/step network-replayer)]
                 (assoc player
                        :input-state input-state 
                        :network-replayer replayer))
               (assoc player :input-state local-input-state))))
    players
    players))

(defn send-local-player-inputs! [{:keys [uid input-frames] :as buffer} n]
  (if (>= (count input-frames) n)
    (do (ws-client/broadcast-input-frames! uid input-frames)
        (assoc buffer :input-frames []))
    buffer))

