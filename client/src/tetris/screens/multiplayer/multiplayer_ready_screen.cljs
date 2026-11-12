(ns tetris.screens.multiplayer.multiplayer-ready-screen
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.audio :as audio]
    [tetris.i18n.core :as i18n]
    [tetris.online.api :as api]
    [tetris.online.room-api :as room-api]
    [tetris.screens.controls-options-screen :as controls-options-screen]
    [tetris.screens.gameplay-options-screen :as gameplay-options-screen]
    [tetris.screens.multiplayer.multiplayer-play-screen :as multiplayer-play-screen]
    [tetris.screens.multiplayer.player-slot :refer [player-slots]]
    [tetris.screens.multiplayer.room-manager :as room-manager]
    [tetris.screens.multiplayer.room-settings :refer [room-settings-view]]
    [tetris.screens.multiplayer.start-button :refer [start-button]]
    [tetris.screens.screen-navigator :as screen-navigator]
    [tetris.screens.tip-buttons :refer [tip-buttons-overlay]]
    [tetris.ui.button :refer [button]]))

(defstyles screen-intro []
  {:position "absolute"
   :top "1rem"
   :left "1rem"
   :font-size "2.2rem"})

(defstyles room-info-class []
  {:position "absolute"
   :top "1rem"
   :left "50%"
   :transform "translateX(-50%)"
   ".room-no" {:font-size "2rem" :user-select "text"}
   ".is-public" {:margin-top "1rem" :color "#999"}})

(defn- handle-leave-room []
  (audio/play :multiplayer/player-left)
  (screen-navigator/pop-screen!)
  (room-api/leave-room))

(defn- handle-room-settings-change [state room]
  (let [settings (select-keys room [:public :game-settings])]
    (swap! state merge settings)
    (room-api/change-room-settings settings)))

(defn listen-events [state]
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
     :on-game-entered (fn [room]
                        (screen-navigator/push-screen!
                          (multiplayer-play-screen/make room)))}))

(defn- build [{:keys [state]}]
  (let [{:keys [room]} @state
        players (room-manager/slot-players @state)
        local-user (api/local-user)
        local-player (room-manager/local-player @state (:id local-user))
        host? (= (:host room) (:id local-user))]
    [:div
     [:div {:class (screen-intro)} "多人游戏房间"]
     [:div {:class (room-info-class)}
      [:div {:class "room-no" :title "房号"} (:id room)]
      [:div {:class "is-public"} (if (:public room) "公开" "不公开")]]
     (room-settings-view room host? #(handle-room-settings-change state %))
     (player-slots room players local-user)
     [:div {:style {:margin-top "2rem"}}
      (button {:bg-color :danger :full? false
               :on-click handle-leave-room}
              (i18n/text [:multiplayer :leave]))
      (start-button local-user room players)]
     (tip-buttons-overlay ["Ctrl-S"
                           (str "切换为" (if (= (:role local-player) :player) "观众" "玩家"))]
                          ["Ctrl-O" "控制设置"]
                          ["Ctrl-G" "游戏设置"])]))

(defn- on-entering [{:keys [state]}]
  (listen-events state)
  (audio/load-sounds :multiplayer)
  (when (= (count (get-in @state [:room :players])) 1)
    (audio/play :multiplayer/player-joined)))

(defn- on-resuming [screen room-id]
  (.then (room-api/join-room room-id)
         (fn [{{:keys [room users]} :data}]
           (reset! (:state screen) (room-manager/make room users))
           (listen-events (:state screen)))))

(defn- on-keydown [{:keys [state]} event]
  (when (.-ctrlKey event)
    (case (.-code event)
      "KeyO" (screen-navigator/push-screen! (controls-options-screen/make))
      "KeyG" (screen-navigator/push-screen! (gameplay-options-screen/make))
      "KeyS" (let [local-player (room-manager/local-player @state (:id (api/local-user)))]
               (room-api/change-role (if (= (:role local-player) :player) :spectator :player)))
      nil)))

(defn make [{:keys [room users]}]
  {:name :multiplayer-ready
   :build build
   :on-entering on-entering
   :on-resuming #(on-resuming % (:id room))
   :on-suspending room-manager/unlisten-events
   :on-exiting (fn []
                 (room-manager/unlisten-events)
                 (room-api/leave-room))
   :on-keydown on-keydown
   :state (r/atom (room-manager/make room users))})

