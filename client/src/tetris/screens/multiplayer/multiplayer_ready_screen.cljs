(ns tetris.screens.multiplayer.multiplayer-ready-screen
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/button" :refer [Button]]
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.audio :as audio]
    [tetris.i18n.core :as i18n]
    [tetris.online.api :as api]
    [tetris.screens.multiplayer.player-slot :refer [player-slots]]
    [tetris.screens.multiplayer.room-manager :as room-manager]
    [tetris.screens.multiplayer.start-button :refer [start-button]]
    [tetris.screens.screen-navigator :as screen-navigator]
    [tetris.screens.styles.button :as styles.button]))

(defstyles room-info-class []
  {:position "absolute"
   :top "1rem"
   :left "50%"
   :transform "translateX(-50%)"
   :font-size "2rem"})

(defn- build [{:keys [state]}]
  (let [{:keys [room]} @state
        players (room-manager/slot-players @state)]
    [:div
     [:div {:class (room-info-class)} (:id room)]
     (player-slots room players)
     [:div {:style {:margin-top "2rem"}}
      [:> Button {:class (styles.button/overlay-button {:bg-color :danger :full? false})
                  :on-click #(screen-navigator/pop-screen!)}
       (i18n/text [:multiplayer :leave])]
      (start-button (api/local-user) room players)]]))

(defn- on-entering [{:keys [state]}]
  (audio/load-sounds :multiplayer)
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
                                            :multiplayer/player-unready))))))}))

(defn make [{:keys [room users]}]
  {:name :multiplayer-ready
   :build build
   :on-entering on-entering
   :on-exiting (fn []
                 (room-manager/exit)
                 (audio/play :multiplayer/player-left))  
   :state (r/atom (room-manager/make room users))})

