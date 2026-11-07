(ns tetris.screens.multiplayer.multiplayer-ready-screen
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.audio :as audio]
    [tetris.i18n.core :as i18n]
    [tetris.online.api :as api]
    [tetris.screens.multiplayer.player-slot :refer [player-slots]]
    [tetris.screens.multiplayer.room-manager :as room-manager]
    [tetris.screens.multiplayer.start-button :refer [start-button]]
    [tetris.screens.multiplayer.multiplayer-play-screen :as multiplayer-play-screen]
    [tetris.screens.screen-navigator :as screen-navigator]
    [tetris.ui.button :refer [button]]))

(defstyles room-info-class []
  {:position "absolute"
   :top "1rem"
   :left "50%"
   :transform "translateX(-50%)"
   ".room-no" {:font-size "2rem" :user-select "text"}
   ".is-public" {:margin-top "1rem" :color "#999"}})

(defn- build [{:keys [state]}]
  (let [{:keys [room]} @state
        players (room-manager/slot-players @state)]
    [:div
     [:div {:class (room-info-class)}
      [:div {:class "room-no" :title "房号"} (:id room)]
      [:div {:class "is-public"} (if (:public room) "公开" "不公开")]]
     (player-slots room players (api/local-user))
     [:div {:style {:margin-top "2rem"}}
      (button {:bg-color :danger :full? false
               :on-click #(screen-navigator/pop-screen!)}
              (i18n/text [:multiplayer :leave]))
      (start-button (api/local-user) room players)]]))

(defn- on-entering [{:keys [state]}]
  (room-manager/listen-events
    state
    {:on-player-joined #(audio/play :multiplayer/player-joined)
     :on-player-left #(audio/play :multiplayer/player-left)
     :on-player-ready (fn [room player]
                        (let [ready? (= (:state player) :readied)]
                          (if (:all-readied? (room-manager/room-stats room))
                            (audio/play :multiplayer/player-ready-all)
                            (when (= (:id player) (:id (api/local-user)))
                              (audio/play (if ready?
                                            :multiplayer/player-ready
                                            :multiplayer/player-unready))))))
     :on-game-entered (fn [_room]
                        (screen-navigator/push-screen!
                          (multiplayer-play-screen/make :marathon)))})
  (audio/load-sounds :multiplayer)
  (when (= (count (get-in @state [:room :players])) 1)
    (audio/play :multiplayer/player-joined)))

(defn make [{:keys [room users]}]
  {:name :multiplayer-ready
   :build build
   :on-entering on-entering
   :on-exiting (fn []
                 (room-manager/exit)
                 (audio/play :multiplayer/player-left))  
   :state (r/atom (room-manager/make room users))})

