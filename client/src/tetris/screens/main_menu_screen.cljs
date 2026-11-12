(ns tetris.screens.main-menu-screen
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.audio :as audio]
    [tetris.online.api :as api]
    [tetris.online.room-api :as room-api]
    [tetris.screens.controls-options-screen :as controls-options-screen]
    [tetris.screens.gameplay-options-screen :as gameplay-options-screen]
    [tetris.screens.login :refer [logic-overlay]]
    [tetris.screens.multiplayer.multiplayer-ready-screen :as multiplayer-ready-screen]
    [tetris.screens.multiplayer.room-create :refer [room-create-dialog]]
    [tetris.screens.multiplayer.room-join :refer [room-join-dialog]]
    [tetris.screens.screen-navigator :as screen-navigator]
    [tetris.screens.solo-play-screen :as solo-play-screen]
    [tetris.screens.tip-buttons :refer [tip-buttons-overlay]]
    [tetris.screens.user :refer [display-user]]
    [tetris.ui.menu :as menu]
    [tetris.ui.menu-overlay :refer [menu-overlay]]
    [tetris.online.ws-client :as ws-client]))

(defstyles menu-item-class [item-id]
  {:margin-top (if (= item-id :back) "1em" 0)
   :font-size "2rem"
   :color "#999"})

(defstyles menu-item-hover-class [item-id]
  {:color "#111"})

(defstyles header-class []
  {:position "absolute"
   :top 0
   :display "flex"
   :width "100%"
   :height "40px"})

(defstyles online-stats-class []
  {:margin-left "0.4rem"
   :margin-top "0.4rem"
   ":nth-child(2)" {:margin-left "0.2rem"}})

(defn- ^:async join-room [room-id quick-join?]
  (let [resp (await (room-api/join-room room-id quick-join?))]
    (if (= (:code resp) :ok)
      (screen-navigator/push-screen! (multiplayer-ready-screen/make (:data resp)))
      (js/alert "未找到房间"))))

(defn- build [{:keys [state]}]
  (let [{:keys [online-count login-opened room-create-opened room-join-opened menu-state]} @state
        api-state (api/get-state)
        local-user (api/local-user)]
    [:div
     [:div {:class (header-class)}
      [display-user api-state local-user #(swap! state update :login-opened not)]
      [:div {:class (online-stats-class)}
       [:span "在线:"]
       [:span online-count]]
      (when login-opened
        [logic-overlay #(swap! state assoc :login-opened false)])]
     (when room-create-opened
       [room-create-dialog (fn [{:keys [room users]}]
                             (swap! state assoc :room-create-opened false)
                             (when room
                               (screen-navigator/push-screen!
                                 (multiplayer-ready-screen/make {:room room :users users}))))])
     (when room-join-opened
       [room-join-dialog (fn [{:keys [room users]}]
                           (swap! state assoc :room-join-opened false)
                           (when room
                             (screen-navigator/push-screen!
                               (multiplayer-ready-screen/make {:room room :users users}))))])
     [menu-overlay
      {:state menu-state
       :on-select (fn [{:keys [id parent] screen-maker :data}]
                    (if (contains? #{:marathon :classic :40L} id)
                      (audio/play :menu-gameplay-select)
                      (when (not (menu/menu? menu-state id)) 
                        (audio/play :menu-select)))
                    (when (= id :multiplayer)
                      (audio/load-sounds :multiplayer))
                    (cond
                      (and (= parent :multiplayer) (= (api/get-state) :offline))
                      (swap! state assoc :login-opened true)

                      (= id :quick-join-room) (join-room nil true)
                      (= id :join-room) (swap! state assoc :room-join-opened true)
                      (= id :create-room) (swap! state assoc :room-create-opened true)

                      :else (when screen-maker (screen-navigator/push-screen! (screen-maker)))))
       :menu-item-class menu-item-class
       :menu-item-hover-class menu-item-hover-class}]
     (tip-buttons-overlay)]))

(defn- on-keydown [{:keys [state]} event]
  (when (= (.-code event) "Escape")
    (swap! state assoc
           :login-opened false
           :room-create-opened false
           :room-join-opened false))
  true)

(defn make []
  (let [load-online-stats
        (fn [{:keys [state]}]
          (when (= (api/get-state) :online)
            (.then (api/get-online-stats)
                   (fn [{{:keys [online-count]} :data}]
                     (swap! state assoc :online-count online-count)))))
        start-poll-timer
        (fn [screen]
          (load-online-stats screen)
          (swap! (:state screen) assoc :timer (js/setInterval #(load-online-stats screen) 3000)))
        stop-poll-timer
        (fn [{:keys [state]}] (js/clearInterval (:timer @state)))]
    {:name :main-menu
     :build build
     :on-entering (fn [screen]
                    (ws-client/subscribe! :connected #(load-online-stats screen)))
     :on-suspending stop-poll-timer
     :on-resuming start-poll-timer
     :on-visible (fn [screen visible?]
                   (if visible?
                     (start-poll-timer screen)
                     (stop-poll-timer screen)))
     :on-keydown on-keydown
     :state (r/atom
              {:menu-state
               (r/atom
                 (menu/make
                   [(menu/item :solo nil 1)
                    (menu/item :multiplayer nil 2)
                    (menu/item :quick-join-room :multiplayer 1)
                    (menu/item :join-room :multiplayer 2)
                    (menu/item :create-room :multiplayer 3)
                    #_(menu/item :list-rooms :multiplayer 4 {:screen-name :list-rooms})
                    (menu/item :options nil 3)
                    (menu/item :marathon :solo 1 #(solo-play-screen/make :marathon))
                    (menu/item :classic :solo 2 #(solo-play-screen/make :classic))
                    #_(menu/item :40L :solo 3 {:screen-name :solo :mode :40L})
                    (menu/item :controls-options :options 1 controls-options-screen/make)
                    (menu/item :gameplay-options :options 2 gameplay-options-screen/make)
                    #_(menu/item :audio-options :options 3 {:screen-name :audio-options})]
                   :solo))
               :login-opened false
               :room-create-opened false
               :room-join-opened false
               :online-count nil
               :timer 0})}))
