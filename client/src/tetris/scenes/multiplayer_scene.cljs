(ns tetris.scenes.multiplayer-scene
  (:require [tetris.assets :as assets]
            [tetris.audio :as audio]
            [tetris.core.game :as game]
            [tetris.core.input :as input]
            [tetris.core.rs :as rs]
            [tetris.core.ruleset.rotation-nrs]
            [tetris.core.ruleset.rotation-prs]
            [tetris.core.ruleset.rotation-srs]
            [tetris.input.keyboard :as keyboard]
            [tetris.online.ws-client :as ws-client]
            [tetris.options :as options]
            [tetris.scenes.multiplayer :as multiplayer]
            [tetris.scenes.network-replay :as network-replay]
            [tetris.scenes.render.constants :refer [v-screen-width]]
            [tetris.scenes.render.gameplay.game-view :as game-view]
            [tetris.scenes.render.gameplay.sound-effect :as sound-effect]
            [tetris.scenes.render.pixi-app :as app]))

(defonce ^:private scene-state (atom nil))

(defn- tick []
  (let [pressed-buttons (keyboard/key->buttons @keyboard/pressed-keys)
        local-input-state (keyboard/handle pressed-buttons)]
    (swap! scene-state
           (fn [{:keys [players local-uid] :as scene}]
             (-> scene
                 (update :players multiplayer/update-network-inputs local-input-state)
                 (update :players multiplayer/update-player-games)
                 (update :network-buffer
                         (fn [buffer]
                           (let [frame-no (get-in players [local-uid :game-state :frame])]
                             (network-replay/append-input-frame
                               buffer [frame-no pressed-buttons])))))))
    (let [{:keys [players spectators local-uid network-buffer]} @scene-state
          local-game (get-in players [local-uid :game-state])
          local-game-over? (and local-game (game/find-event :game-over (:events local-game)))
          local-playing? (= (get-in players [local-uid :game-status]) :playing)
          playing-player-cnt (count (filter #(= (:game-status %) :playing) (vals players)))
          spectator-cnt (count spectators)]
      (when (and (some? local-game)
                 (or (and local-playing? (> (+ playing-player-cnt spectator-cnt) 1))
                     local-game-over?))
        (let [buffer (multiplayer/send-local-player-inputs! network-buffer (if local-game-over? 0 10))]
          (swap! scene-state assoc :network-buffer buffer)))
      (doseq [[uid {:keys [game-status game-state input-state game-view]}] players
              :let [game-state (assoc game-state :status game-status)]]
        (game-view/render! game-view game-state (or input-state input/empty-state))
        (when (or (= local-uid uid) (= playing-player-cnt 1)
                  (game/find-event :game-over (:events game-state)))
          (sound-effect/handle game-state))))))

(def MPGamePlayerConfig
  [:map
   [:seed some?]
   [:handling [:or options/Handling [:map]]]
   [:piece-style :string]])

(def MPGameConfig
  [:map
   [:mode [:enum :classic :modern]]
   [:speed-level :int]
   [:rs rs/RotationSystem]
   [:preview-count :int]]) 

(def Player
  [:map
   [:id :int]
   [:username :string]
   [:game-config MPGamePlayerConfig]])

(def StartOptions
  [:map
   [:game-config MPGameConfig]
   [:players [:vector Player]]
   [:spectators [:vector Player]]
   [:local-uid :int]])

(defn ^:async load!
  [piece-styles]
  (await (js/Promise.all [(assets/load-piece-styles piece-styles)
                          (audio/load-sounds :gameplay)]))
  (swap! scene-state assoc :app (await (app/init)))
  (keyboard/init))

(defn ^:async start!
  {:malli/schema [:=> [:cat StartOptions] :any]}
  [{:keys [game-config players spectators local-uid] :as options}]
  (tap> ["start game" game-config players])
  (ws-client/subscribe!
    :multiplayer :player-inputs
    (fn [{:keys [uid input-frames]}]
      (swap! scene-state update-in [:players uid :network-replayer]
             (fn [replayer]
               (network-replay/append-input-frames replayer input-frames)))))
  (let [^js app (:app @scene-state)
        scene-players
        (->> (multiplayer/make-players options)
             (map-indexed vector)
             (reduce
               (fn [m [view-i player]]
                 (let [game-view (game-view/create
                                   (assoc game-config
                                          :player-name (:username player)
                                          :piece-style (get-in player [:game-config :piece-style])))
                       {:keys [container layout]} game-view
                       total-w (* (:width layout) (count players))
                       left (/ (- v-screen-width total-w) 2)]
                   (set! (.-x container)
                         (+ left (* view-i (:width layout))))
                   (.addChild (.-stage app) container)
                   (assoc m (:id player) (assoc player :game-view (atom game-view)))))
               {}))]
    (swap! scene-state assoc
           :players scene-players
           :spectators spectators
           :local-uid local-uid
           :network-buffer (network-replay/make-input-buffer local-uid))
    (.. app -ticker (add tick))
    (.appendChild js/document.body (.-canvas app))))

(defn on-player-event [_room player event-type]
  (when (get-in @scene-state [:players (:id player)])
    (swap! scene-state assoc-in [:players (:id player) :game-status]
           ({:player-game-quitted :quitted
             :player-left :left-room
             :player-joined :quitted} event-type)))
  (when (contains? #{:player-game-quitted :player-left} event-type)
    (swap! scene-state update :spectators
           (fn [spectators] (filterv #(not= (:id %) (:id player)) spectators)))))

(defn destroy! []
  (when-let [app (:app @scene-state)]
    (tap> "destroy game")
    (.destroy app #js {:removeView true})
    (swap! scene-state assoc :app nil)))

