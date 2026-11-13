(ns tetris.screens.multiplayer.multiplayer-play-screen
  (:require-macros [cljss.core])
  (:require [cljss.core :as css :refer-macros [defstyles]]
            [reagent.core :as r]
            [tetris.assets :as assets]
            [tetris.online.api :as api]
            [tetris.online.room-api :as room-api]
            [tetris.online.ws-client :as ws-client]
            [tetris.options-store :as options-store]
            [tetris.scenes.game :as game]
            [tetris.scenes.multiplayer :as multiplayer]
            [tetris.screens.multiplayer.room-manager :as room-manager]
            [tetris.screens.screen-navigator :as screen-navigator]))

(defn- loading-overlay []
  [:div {:style {:z-index 3
                 :font-size 24}}
   "加载中..."])

(defstyles spectators-class []
  {:position "absolute"
   :left "1rem"
   :top "2rem"
   :color "#fafafa"
   :text-align "left"
   ".title" {:margin-bottom "0.5rem"}})

(defn- build [{:keys [state]}]
  (let [{:keys [loading? room users]} @state
        spectators (->> (:players room)
                        (mapv #(room-manager/player-info % users))
                        (filterv #(and (= (:role %) :spectator) (= (:state %) :inprogress))))]
    [:div
     (when loading? [loading-overlay])
     (when (seq spectators)
       [:div {:class (spectators-class)}
        [:div {:class "title"}
         (str "观众(" (count spectators) "):")]
        (map (fn [spectator]
               [:div (:username spectator)]) spectators)])]))

(defn- on-entering [screen] 
  (ws-client/subscribe!
    :multiplayer :game-started
    (fn [{:keys [room users]}]
      (swap! (:state screen) assoc :loading? false :room room :users users)
      (let [players (mapv #(room-manager/player-info % users) (:players room))]
        (multiplayer/start!
          {:game-config (-> (:game-settings room)
                            (update :mode keyword)
                            (update :rs keyword))
           :local-uid (:id (api/local-user))
           :players (filterv #(= (:role %) :player) players)
           :spectators (filterv #(= (:role %) :spectator) players)}))))
  (ws-client/subscribe! :multiplayer :disconnectd screen-navigator/pop-screen!)
  (ws-client/subscribe!
    :multiplayer  :player-game-quitted
    (fn [{:keys [room player]}]
      (swap! (:state screen) assoc :room room)
      (when (empty? (filter #(and (= (keyword (:role %)) :player)
                                  (= (keyword (:state %)) :inprogress))
                            (:players room)))
        (screen-navigator/pop-screen!))
      (multiplayer/on-player-event room player :player-game-quitted)))
  (ws-client/subscribe!
    :multiplayer :player-left
    (fn [{:keys [room player]}]
      (swap! (:state screen) assoc :room room)
      (multiplayer/on-player-event room player :player-left)))
  (ws-client/subscribe!
    :multiplayer :player-joined
    (fn [{:keys [room player]}]
      (multiplayer/on-player-event room player :player-joined)))
  (.then (multiplayer/load! (:piece-styles screen))
         #(room-api/request-start-game
            {:seed (game/random-seed)
             :handling (options-store/get-options :handling)
             :piece-style (options-store/get-options :piece-style)})))

(defn- on-keydown [_ event]
  (when (= (.-code event) "Escape")
    (screen-navigator/pop-screen!))
  true)

(defn make [room]
  {:name :multiplayer-play
   :build build
   :on-entering on-entering
   :on-exiting (fn []
                 (ws-client/unsubscribe! :multiplayer :all)
                 (multiplayer/destroy!)
                 (room-api/quit-game))
   :on-keydown on-keydown
   :piece-styles (assets/get-piece-styles) 
   :state (r/atom {:loading? true :room room :users []})})

