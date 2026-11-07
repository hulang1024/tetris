(ns tetris.screens.multiplayer.multiplayer-play-screen
  (:require [reagent.core :as r]
            [tetris.options-store :as options-store]
            [tetris.scenes.multiplayer :as multiplayer]
            [tetris.online.api :as api]
            [tetris.online.ws-client :as ws-client]))

(defn- loading-overlay []
  [:div {:style {:z-index 3
                 :font-size 24}}
   "加载中..."])

(defn- build [{:keys [state]}]
  [:div
   (when (:loading? @state) [loading-overlay])])

(defn- on-entering [this] 
  (ws-client/subscribe!
    :game-started
    (fn []
      (swap! (:state this) assoc :loading? false)
      (multiplayer/start!)))
  (.then (multiplayer/load (:scene-options this))
         #(api/post "/room/request-start-game")))

(defn make [mode]
  {:name :multiplayer-play
   :build build
   :on-entering on-entering
   :on-exiting multiplayer/destroy
   :scene-options {:mode mode
                   :seed (+ (js/Date.now))
                   :options (if (not= mode :classic)
                              (options-store/get-options :handling)
                              {})}
   :state (r/atom {:loading? true})})

