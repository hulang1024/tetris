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
            [tetris.scenes.gameplay :as gameplay]
            [tetris.scenes.render.pixi-app :as app]
            [tetris.screens.menu.core :as menu]
            [tetris.screens.menu.menu-overlay :refer [menu-overlay]]
            [tetris.screens.replay-screen :as replay-screen]
            [tetris.screens.screen-stack :as screen-stack]
            [tetris.screens.controls-options-screen :as controls-options-screen]))

(def pixi-app (atom nil))

(defn- destroy-scene []
  (when @pixi-app
    (gameplay/destroy)
    (.destroy @pixi-app #js {:removeView true})
    (reset! pixi-app nil)))

(defn- ^:async load-scene [{:keys [mode seed options]}]
  (destroy-scene)
  (keyboard/init)
  (reset! pixi-app (await (app/init)))
  (.appendChild js/document.body (.-canvas @pixi-app))
  (let [game-state (tick/initial-game
                     (merge (case mode
                              :marathon (make-modern-ruleset seed 1)
                              :classic (make-classic-ruleset seed 0))
                            options))]
    (await (gameplay/start @pixi-app game-state))))

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

(declare make)

(defn- build [{this :state}]
  [:div
   (cond
     (:loading? @this) [loading-overlay]

     (contains? #{:pause :game-over} (:game-status @this))
     [menu-overlay
      {:menu-items
       (->> [(menu/item :start-over nil 1)
             (menu/item :continue-game nil 2)
             (menu/item :custom-controls nil 3)
             (menu/item :replay nil 4)
             (menu/item :exit-game nil 5)]
            (filterv (if (= (:game-status @this) :game-over)
                       #(not= (:id %) :continue-game)
                       identity)))
       :default-hover :continue-game
       :on-enter
       (fn [item]
         (when (contains? #{:start-over} (:id item))
           (audio/play :gameplay))
         (case (:id item)
           :continue-game (do (swap! this assoc :game-status :playing)
                              (swap! (:scene-state @this) assoc :status :playing))
           :start-over (screen-stack/replace-screen!
                         (make (select-keys (:settings @this) [:mode])))
           :custom-controls (screen-stack/push-screen! (controls-options-screen/make))
           :exit-game (screen-stack/pop-screen!)
           :replay (screen-stack/push-screen!
                     (replay-screen/make
                       (assoc (:settings @this)
                              :replay-events (:replay-recorder @(:scene-state @this)))))
           nil))
       :menu-class gameplay-menu-class
       :menu-item-class menu-item-class
       :menu-item-hover-class menu-item-hover-class}]

     :else nil)])

(defn- on-entering [{this :state}] 
  (when (contains? #{:marathon :classic} (get-in @this [:settings :mode]))
    (let [scene-state-promise (load-scene (:settings @this))]
      (.then scene-state-promise
             (fn [scene-state]
               (swap! this assoc
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
                       (swap! this
                              (fn [screen]
                                (assoc screen
                                       :game-status (:status new)))))))))))))

(defn- on-suspending []
  (gstyle/setStyle (.-canvas @pixi-app) #js {:display "none"})) 

(defn- on-resuming []
  (gstyle/setStyle (.-canvas @pixi-app) #js {:display "block"})) 

(defn- on-exiting []
  (destroy-scene))
(defn- on-keydown [{this :state} event]
  (when (= (.-code event) "Escape")
    (if (= (:status @(:scene-state @this)) :game-over)
      (screen-stack/pop-screen!)
      (let [{:keys [game-status]} @this
            new-game-status (case game-status
                              :playing :pause
                              :pause :playing
                              game-status)]
        (swap! this assoc :game-status new-game-status)
        (swap! (:scene-state @this) assoc :status new-game-status))))
  true)

(defn make [mode]
  {:name :solo-play
   :build build
   :on-entering on-entering
   :on-suspending on-suspending
   :on-resuming on-resuming
   :on-exiting on-exiting
   :on-keydown on-keydown
   :state (r/atom {:loading? true
                   :settings {:mode mode
                              :seed (+ (js/Date.now))
                              :options (if (not= mode :classic)
                                         (options-store/get-options :handling)
                                         {})} 
                   :scene-state nil
                   :game-status :playing})})

