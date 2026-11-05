(ns tetris.screens.multiplayer.multiplayer-ready-screen
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/button" :refer [Button]]
    [reagent.core :as r]
    [tetris.audio :as audio]
    [tetris.i18n.core :as i18n]
    [tetris.online.api :as api]
    [tetris.screens.multiplayer.player-slot :refer [player-slots]]
    [tetris.screens.multiplayer.room-manager :as room-manager]
    [tetris.screens.multiplayer.start-button :refer [start-button]]
    [tetris.screens.screen-stack :as screen-stack]
    [tetris.screens.styles.button :as styles.button]))

(defn- on-entering [{:keys [state]}]
  (audio/load-sounds :multiplayer)
  (room-manager/listen-events
    state
    {:on-player-joined #(audio/play :multiplayer/player-joined)
     :on-player-left #(audio/play :multiplayer/player-left)
     :on-player-ready (fn [me? ready? all-readied?]
                        (if all-readied?
                          (audio/play :multiplayer/player-ready-all)
                          (when me?
                            (audio/play (if ready?
                                          :multiplayer/player-ready
                                          :multiplayer/player-unready)))))}))

(defn- build [{:keys [state]}]
  (let [{:keys [room players]} @state]
    [:div
     (player-slots room players)
     [:div {:style {:margin-top "2rem"}}
      [:> Button {:class (styles.button/overlay-button {:bg-color :danger :full? false})
                  :on-click #(screen-stack/pop-screen!)}
       (i18n/text [:multiplayer :leave])]
      (start-button (api/local-user) room players)]]))

(defn make [{:keys [room users]}]
  {:name :multiplayer-ready
   :build build
   :on-entering on-entering
   :on-exiting room-manager/exit  
   :state (r/atom (room-manager/make room users))})

