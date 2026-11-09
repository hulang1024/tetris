(ns tetris.screens.multiplayer.multiplayer-play-screen
  (:require [reagent.core :as r]
            [tetris.online.api :as api]
            [tetris.online.ws-client :as ws-client]
            [tetris.options-store :as options-store]
            [tetris.scenes.multiplayer :as multiplayer]
            [tetris.screens.multiplayer.room-manager :as room-manager]
            [tetris.screens.screen-navigator :as screen-navigator]))

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
    (fn [{:keys [room users]}]
      (swap! (:state this) assoc :loading? false)
      (multiplayer/start! {:game (:game this)
                           :local-uid (:id (api/local-user))
                           :players (mapv #(room-manager/player-info % users) (:players room))})))
  (ws-client/subscribe!
    :player-left
    (fn [] (screen-navigator/pop-screen!)))

  (.then (multiplayer/load! (:game this))
         #(room-manager/request-start-game
            {:seed (int (* (rand) 100000))
             :handling (options-store/get-options :handling)})))

(defn make [_room]
  {:name :multiplayer-play
   :build build
   :on-entering on-entering
   :on-exiting (fn []
                 (ws-client/unsubscribe! :game-started :player-left)
                 (multiplayer/destroy!)
                 (room-manager/leave-room))
   :game {:mode :marathon
          :preview-count 4
          :piece-style "b11"} 
   :state (r/atom {:loading? true})})

