(ns tetris.screens.replay-screen
  (:require-macros [cljss.core])
  (:require [cljss.core :as css :refer-macros [defstyles]]
            [reagent.core :as r]
            [tetris.input.keyboard :as keyboard]
            [tetris.scenes.game :refer [make-game-options]]
            [tetris.scenes.render.pixi-app :as app]
            [tetris.scenes.replay :as replay]
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

(defn- ^:async load-scene [state]
  (keyboard/init)
  (swap! state assoc :pixi-app (await (app/init)))
  (let [{:keys [pixi-app game-config replay]} @state]
    (.appendChild js/document.body (.-canvas pixi-app))
    (await (replay/start pixi-app (make-game-options game-config) replay))))

(declare make)

(defn- build [{:keys [state]}]
  [:div
   [:div {:style {:position "absolute"
                  :top "1rem"
                  :color "#fff"}}
    (if (= (:game-status @state) :ended) "回放结束" "回放中")]
   (cond
     (:loading? @state) [loading-overlay]
     (contains? #{:pause :game-over :ended} (:game-status @state))
     [menu-overlay
      {:menu-items
       [(menu/item :start-over nil 1)
        (menu/item :continue-replay nil 2)
        (menu/item :exit-replay nil 3)]
       :default-hover :exit-replay
       :on-select
       (fn [item]
         (case (:id item)
           :continue-replay (do (swap! state assoc :game-status :playing)
                                (swap! (:scene-state @state) assoc :status :playing))
           :start-over (screen-navigator/replace-screen! (make (:game-config @state) (:replay @state)))
           :exit-replay (screen-navigator/pop-screen!)
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

(defn- on-exiting [{:keys [state]}]
  (when (:pixi-app @state)
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

(defn make [game-config replay]
  {:name :replay
   :build build
   :on-entering on-entering
   :on-exiting on-exiting
   :on-keydown on-keydown
   :state (r/atom {:pixi-app nil
                   :loading? true
                   :game-config game-config
                   :replay replay
                   :scene-state nil
                   :game-status :playing})})

