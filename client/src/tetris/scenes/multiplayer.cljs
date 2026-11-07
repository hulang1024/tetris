(ns tetris.scenes.multiplayer
  (:require [tetris.assets :as assets]
            [tetris.audio :as audio]
            [tetris.core.game :as game]
            [tetris.core.ruleset.rotation-nrs]
            [tetris.core.ruleset.rotation-prs]
            [tetris.core.ruleset.rotation-srs]
            [tetris.core.ruleset.classic :refer [make-classic-ruleset]]
            [tetris.core.ruleset.modern :refer [make-modern-ruleset]]
            [tetris.core.tick :as tick]
            [tetris.input.keyboard :as keyboard]
            [tetris.options :as options]
            [tetris.scenes.render.gameplay.game-view :as game-view]
            [tetris.scenes.render.gameplay.sound-effect :as sound-effect]
            [tetris.scenes.render.pixi-app :as app]))

(defn- initial-scene [game-state]
  {:game-state game-state
   :status :loading ; :loading :playing :game-over
   :game-view nil
   :app nil})

(def scene-state (atom nil))

(defn- tick []
  (let [pressed-buttons (keyboard/key->buttons @keyboard/pressed-keys)
        input-state (keyboard/handle pressed-buttons)]
    (swap! scene-state
           (fn [scene-state]
             (let [{:keys [status game-state]} scene-state]
               (case status
                 :game-over
                 (assoc scene-state
                        :game-state (assoc game-state :events []))

                 :playing
                 (let [game-state (-> (assoc game-state :events [])
                                      (tick/step input-state))
                       status (if (game/find-event :game-over (:events game-state))
                                :game-over
                                status)]
                   (assoc scene-state
                          :status status
                          :game-state game-state))

                 scene-state))))
    (when (not= (:status @scene-state) :loading)
      (let [{:keys [game-state game-view]} @scene-state]
        (game-view/render! game-view game-state input-state)
        (sound-effect/handle game-state)))))

(def SceneOptions
  [:map
   [:mode [:enum :classic :marathon]]
   [:seed some?]
   [:options [:maybe options/Handling]]])

(defn ^:async load
  {:malli/schema [:=> [:cat SceneOptions] :any]}
  [{:keys [mode seed options]}]
  (keyboard/init)
  (let [^js app (await (app/init))
        game-state (tick/initial-game
                     (merge (case mode
                              :marathon (make-modern-ruleset seed 1)
                              :classic (make-classic-ruleset seed 0))
                            options))
        options (merge game-state {:piece-style "b11"})]
    (await (js/Promise.all [(assets/load-piece-styles (:piece-style options))
                            (audio/load-sounds :gameplay)]))
    (let [view (game-view/create options)]
      (.addChild (.-stage app) (:container view))
      (reset! scene-state
              (assoc (initial-scene game-state)
                     :game-view (atom view)
                     :app app))
      (.. app -ticker (add tick))
      scene-state)))

(defn ^:async start! []
  (tap> "start game")
  (.appendChild js/document.body (.-canvas (:app @scene-state)))
  (swap! scene-state assoc :status :playing))

(defn destroy []
  (when (:app @scene-state)
    (.destroy (:app @scene-state) #js {:removeView true})
    (swap! scene-state assoc :app nil)))
