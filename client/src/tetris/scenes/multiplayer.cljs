(ns tetris.scenes.multiplayer
  (:require [tetris.assets :as assets]
            [tetris.audio :as audio]
            [tetris.core.game :as game]
            [tetris.core.input :as input]
            [tetris.core.rs :as rs]
            [tetris.core.ruleset.classic :refer [make-classic-ruleset]]
            [tetris.core.ruleset.modern :refer [make-modern-ruleset]]
            [tetris.core.ruleset.rotation-nrs]
            [tetris.core.ruleset.rotation-prs]
            [tetris.core.ruleset.rotation-srs]
            [tetris.core.tick :as tick]
            [tetris.input.keyboard :as keyboard]
            [tetris.online.network-replay :as network-replay]
            [tetris.online.ws-client :as ws-client]
            [tetris.options :as options]
            [tetris.scenes.render.constants :refer [v-screen-width]]
            [tetris.scenes.render.gameplay.game-view :as game-view]
            [tetris.scenes.render.gameplay.sound-effect :as sound-effect]
            [tetris.scenes.render.pixi-app :as app]))

(defn- make-players [{:keys [players local-uid]
                      {:keys [mode speed-level rs]} :game}]
  (mapv
    (fn [{:keys [id game-config] :as player}]
      (let [game-state
            (tick/initial-game
              (merge (case mode
                       :modern (make-modern-ruleset (:seed game-config) speed-level)
                       :classic (make-classic-ruleset (:seed game-config) speed-level))
                     {:rotation-system rs}
                     (:handling game-config)))]
        {:id id
         :name (:username player)
         :game-state game-state
         :game-status :playing
         :game-view nil
         :network-replayer (when (not= local-uid id)
                             (network-replay/make-replayer id))
         :input-state nil}))
    players))

(defn- update-player-games [players]
  (reduce-kv
    (fn [players uid {:keys [input-state game-state game-status] :as player}]
      (if input-state
        (let [game-state (-> (assoc game-state :events [])
                             (tick/step input-state))
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

(defn- update-network-inputs [players local-input-state]
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

(defonce ^:private scene-state (atom {:playing? false}))

(defn- tick []
  (let [pressed-buttons (keyboard/key->buttons @keyboard/pressed-keys)
        local-input-state (keyboard/handle pressed-buttons)]
    (swap! scene-state
           (fn [scene]
             (if (:playing? scene)
               (let [{:keys [players local-uid]} @scene-state
                     local-game (get-in players [local-uid :game-state])]
                 (-> scene
                     (update :players update-network-inputs local-input-state)
                     (update :players update-player-games)
                     (update :network-buffer
                             (fn [buffer]
                               (network-replay/append-input-frame
                                 buffer [(:frame local-game) pressed-buttons])))))
               scene)))
    (let [{:keys [playing? players local-uid network-buffer]} @scene-state
          local-playing? (= (get-in players [local-uid :game-status]) :playing)
          playing-player-cnt (count (filter #(= (:game-status %) :playing) (vals players)))]
      (when playing?
        (when (and local-playing? (> playing-player-cnt 1))
          (let [buffer (network-replay/send! network-buffer 10)]
            (swap! scene-state assoc :network-buffer buffer)))
        (doseq [[uid {:keys [game-status game-state input-state game-view]}] players
                :let [game-state (assoc game-state :status game-status)]]
          (game-view/render! game-view game-state (or input-state input/empty-state))
          (when (or (= local-uid uid) (= playing-player-cnt 1))
            (sound-effect/handle game-state)))))))

(def Player
  [:map
   [:id :int]
   [:username :string]
   [:game-config
    [:map
     [:seed some?]
     [:handling [:or options/Handling [:map]]]]]])

(def Game
  [:map
   [:mode [:enum :classic :modern]]
   [:speed-level :int]
   [:rs rs/RotationSystem]
   [:preview-count :int]
   [:piece-style :string]])

(def StartOptions
  [:map
   [:game Game]
   [:players [:vector Player]]
   [:local-uid :int]])

(defn ^:async load!
  [piece-style]
  (await (js/Promise.all [(assets/load-piece-styles piece-style)
                          (audio/load-sounds :gameplay)]))
  (swap! scene-state assoc :app (await (app/init)))
  (keyboard/init))

(defn ^:async start!
  {:malli/schema [:=> [:cat StartOptions] :any]}
  [{:keys [game players local-uid] :as options}]
  (tap> ["start game" game players])
  (ws-client/subscribe!
    :player-inputs
    (fn [{:keys [uid input-frames]}]
      (swap! scene-state update-in [:players uid :network-replayer]
             (fn [replayer]
               (network-replay/append-input-frames replayer input-frames)))))
  (let [^js app (:app @scene-state)
        scene-players
        (->> (make-players options)
             (map-indexed vector)
             (reduce
               (fn [m [view-i player]]
                 (let [game-view (game-view/create
                                   (assoc game :player-name (:name player)))
                       {:keys [container layout]} game-view
                       total-w (* (:width layout) (count players))
                       left (/ (- v-screen-width total-w) 2)]
                   (set! (.-x container)
                         (+ left (* view-i (:width layout))))
                   (.addChild (.-stage app) container)
                   (assoc m (:id player) (assoc player :game-view (atom game-view)))))
               {}))]
    (swap! scene-state assoc
           :playing? true
           :players scene-players
           :local-uid local-uid
           :network-buffer (network-replay/make-input-buffer local-uid))
    (.. app -ticker (add tick))
    (.appendChild js/document.body (.-canvas app))))

(defn on-player-event [_room player event-type]
  (when (get-in @scene-state [:players (:id player)])
    (swap! scene-state assoc-in [:players (:id player) :game-status]
           ({:player-game-quitted :quitted
             :player-left :left-room
             :player-joined :quitted} event-type))))

(defn destroy! []
  (when-let [app (:app @scene-state)]
    (tap> "destroy game")
    (.destroy app #js {:removeView true})
    (swap! scene-state assoc :app nil)))

