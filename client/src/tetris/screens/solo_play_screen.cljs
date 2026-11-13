(ns tetris.screens.solo-play-screen
  (:require-macros [cljss.core])
  (:require [cljss.core :as css :refer-macros [defstyles]]
            [goog.style :as gstyle]
            [reagent.core :as r]
            [tetris.audio :as audio]
            [tetris.input.keyboard :as keyboard]
            [tetris.options-store :as options-store]
            [tetris.scenes.game :as game :refer [make-game-options
                                                 mode->game-config]]
            [tetris.scenes.render.pixi-app :as app]
            [tetris.scenes.solo :as solo]
            [tetris.screens.controls-options-screen :as controls-options-screen]
            [tetris.screens.gameplay-options-screen :as gameplay-options-screen]
            [tetris.screens.replay-screen :as replay-screen]
            [tetris.screens.screen-navigator :as screen-navigator]
            [tetris.ui.menu :as menu]
            [tetris.ui.menu-overlay :refer [menu-overlay]]))

(defstyles gameplay-menu-class []
  {:background "rgba(0, 0, 0, 0.6)"
   :backdrop-filter "blur(0.1em)"
   :padding "1em 2em"})

(defstyles menu-item-class [item-id]
  {:color "#aaa"})

(defstyles menu-item-hover-class [item-id]
  {:color "#fff"})

(defn- loading-overlay []
  [:div {:style {:z-index 3
                 :font-size 24}}
   "加载中..."])

(defn update-game-config! [state]
  (let [game-config (assoc (:game-config @state)
                           :seed (game/random-seed)
                           :handling (options-store/get-options :handling)
                           :piece-style (options-store/get-options :piece-style))]
    (swap! state assoc :game-config game-config)))

(defn- ^:async load-scene [state]
  (keyboard/init)
  (swap! state assoc :pixi-app (await (app/init)))
  (update-game-config! state)
  (.appendChild js/document.body (.-canvas (:pixi-app @state)))
  (await (solo/start (:pixi-app @state) (make-game-options (:game-config @state)))))

(declare make)

(defn- build [{:keys [state]}]
  [:div
   (cond
     (:loading? @state) [loading-overlay]

     (contains? #{:pause :game-over} (:game-status @state))
     [menu-overlay
      {:menu-items
       (->> [(menu/item :start-over nil 1)
             (menu/item :continue-game nil 2)
             (menu/item :custom-controls nil 3)
             (menu/item :custom-gameplay nil 3)
             (menu/item :replay nil 4)
             (menu/item :exit-game nil 5)]
            (filterv (if (= (:game-status @state) :game-over)
                       #(not= (:id %) :continue-game)
                       identity)))
       :default-hover :continue-game
       :on-select
       (fn [item]
         (when (contains? #{:start-over} (:id item))
           (audio/play :gameplay))
         (case (:id item)
           :continue-game (do (swap! state assoc :game-status :playing)
                              (swap! (:scene-state @state) assoc :status :playing))
           :start-over (screen-navigator/replace-screen! (make (update-game-config! state)))
           :custom-controls (screen-navigator/push-screen! (controls-options-screen/make))
           :custom-gameplay (screen-navigator/push-screen! (gameplay-options-screen/make true))
           :exit-game (screen-navigator/pop-screen!)
           :replay (screen-navigator/push-screen!
                     (replay-screen/make 
                       (:game-config @state)
                       (:replay-recorder @(:scene-state @state))))
           nil))
       :menu-class gameplay-menu-class
       :menu-item-class menu-item-class
       :menu-item-hover-class menu-item-hover-class}]

     :else nil)])

(defn- on-entering [{:keys [state]}] 
  (let [scene-state-promise (load-scene state)]
    (.then scene-state-promise
           (fn [scene-state]
             (swap! state assoc
                    :loading? false
                    :scene-state scene-state)
             (add-watch
               scene-state
               :sync
               (fn [_key _ref old new]
                 (let [listen-keys [:status]
                       old (select-keys old listen-keys)
                       new (select-keys new listen-keys)]
                   (when (not= old new)
                     (swap! state assoc :game-status (:status new))))))))))

(defn- on-suspending [{:keys [state]}]
  (gstyle/setStyle (.-canvas (:pixi-app @state)) #js {:display "none"})) 

(defn- on-resuming [{:keys [state]}]
  (gstyle/setStyle (.-canvas (:pixi-app @state)) #js {:display "block"})) 

(defn- on-exiting [{:keys [state]}]
  (when (:pixi-app @state)
    (solo/destroy)
    (.destroy (:pixi-app @state) #js {:removeView true})
    (swap! state assoc :pixi-app nil)))

(defn- on-keydown [{:keys [state]} event]
  (when (= (.-code event) "Escape")
    (if (= (:status @(:scene-state @state)) :game-over)
      (screen-navigator/pop-screen!)
      (let [{:keys [game-status]} @state
            new-game-status (case game-status
                              :playing :pause
                              :pause :playing
                              game-status)]
        (swap! state assoc :game-status new-game-status)
        (swap! (:scene-state @state) assoc :status new-game-status))))
  true)

(defn make [game]
  {:name :solo-play
   :build build
   :on-entering on-entering
   :on-suspending on-suspending
   :on-resuming on-resuming
   :on-exiting on-exiting
   :on-keydown on-keydown
   :state (r/atom {:pixi-app nil
                   :game-config (if (keyword? game) (mode->game-config game) game)
                   :loading? true
                   :scene-state nil
                   :game-status :playing})})

