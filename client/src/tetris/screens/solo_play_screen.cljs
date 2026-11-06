(ns tetris.screens.solo-play-screen
  (:require-macros [cljss.core])
  (:require [cljss.core :as css :refer-macros [defstyles]]
            [goog.style :as gstyle]
            [reagent.core :as r]
            [tetris.audio :as audio]
            [tetris.core.ruleset.classic :refer [make-classic-ruleset]]
            [tetris.core.ruleset.modern :refer [make-modern-ruleset]]
            [tetris.core.tick :as tick]
            [tetris.input.keyboard :as keyboard]
            [tetris.options-store :as options-store]
            [tetris.ui.menu :as menu]
            [tetris.ui.menu-overlay :refer [menu-overlay]]
            [tetris.scenes.gameplay :as gameplay]
            [tetris.scenes.render.pixi-app :as app]
            [tetris.screens.replay-screen :as replay-screen]
            [tetris.screens.screen-navigator :as screen-navigator]
            [tetris.screens.controls-options-screen :as controls-options-screen]))

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
  (let [{{:keys [mode seed options]} :settings} @state]
    (keyboard/init)
    (swap! state assoc :pixi-app (await (app/init)))
    (.appendChild js/document.body (.-canvas (:pixi-app @state)))
    (let [game-state (tick/initial-game
                       (merge (case mode
                                :marathon (make-modern-ruleset seed 1)
                                :classic (make-classic-ruleset seed 0))
                              options))]
      (await (gameplay/start (:pixi-app @state) game-state)))))

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
           :start-over (screen-navigator/replace-screen!
                         (make (select-keys (:settings @state) [:mode])))
           :custom-controls (screen-navigator/push-screen! (controls-options-screen/make))
           :exit-game (screen-navigator/pop-screen!)
           :replay (screen-navigator/push-screen!
                     (replay-screen/make
                       (assoc (:settings @state)
                              :replay-events (:replay-recorder @(:scene-state @state)))))
           nil))
       :menu-class gameplay-menu-class
       :menu-item-class menu-item-class
       :menu-item-hover-class menu-item-hover-class}]

     :else nil)])

(defn- on-entering [{:keys [state]}] 
  (when (contains? #{:marathon :classic} (get-in @state [:settings :mode]))
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
                       (swap! state assoc :game-status (:status new)))))))))))

(defn- on-suspending [{:keys [state]}]
  (gstyle/setStyle (.-canvas (:pixi-app @state)) #js {:display "none"})) 

(defn- on-resuming [{:keys [state]}]
  (gstyle/setStyle (.-canvas (:pixi-app @state)) #js {:display "block"})) 

(defn- on-exiting [{:keys [state]}]
  (when (:pixi-app @state)
    (gameplay/destroy)
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

(defn make [mode]
  {:name :solo-play
   :build build
   :on-entering on-entering
   :on-suspending on-suspending
   :on-resuming on-resuming
   :on-exiting on-exiting
   :on-keydown on-keydown
   :state (r/atom {:pixi-app nil
                   :loading? true
                   :settings {:mode mode
                              :seed (+ (js/Date.now))
                              :options (if (not= mode :classic)
                                         (options-store/get-options :handling)
                                         {})} 
                   :scene-state nil
                   :game-status :playing})})

