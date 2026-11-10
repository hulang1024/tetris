(ns tetris.screens.multiplayer.multiplayer-play-screen
  (:require [reagent.core :as r]
            [tetris.online.api :as api]
            [tetris.online.room-api :as room-api]
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
      (multiplayer/start! {:game (merge (select-keys this [:piece-style])
                                        (-> (:game-settings room)
                                            (update :mode keyword)
                                            (update :rs keyword)))
                           :local-uid (:id (api/local-user))
                           :players (mapv #(room-manager/player-info % users) (:players room))})))
  (ws-client/subscribe! :closed #(screen-navigator/pop-screen!))
  (ws-client/subscribe! :player-game-quitted
                        (fn [{:keys [room player]}]
                          (multiplayer/on-player-event room player :player-game-quitted)))
  (ws-client/subscribe! :player-left
                        (fn [{:keys [room player]}]
                          (multiplayer/on-player-event room player :player-left)))
  (ws-client/subscribe! :player-joined
                        (fn [{:keys [room player]}]
                          (multiplayer/on-player-event room player :player-joined)))
  (.then (multiplayer/load! (:piece-style this))
         #(room-api/request-start-game
            {:seed (int (* (rand) 100000))
             :handling (options-store/get-options :handling)})))

(defn- on-keydown [_ event]
  (when (= (.-code event) "Escape")
    (.then (room-api/quit-game) #(screen-navigator/pop-screen!)))
  true)

(defn make [_room]
  {:name :multiplayer-play
   :build build
   :on-entering on-entering
   :on-exiting (fn []
                 (ws-client/unsubscribe! :all)
                 (multiplayer/destroy!))
   :on-keydown on-keydown
   :piece-style "b11" 
   :state (r/atom {:loading? true})})

