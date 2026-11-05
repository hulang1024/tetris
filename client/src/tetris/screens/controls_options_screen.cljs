(ns tetris.screens.controls-options-screen
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [clojure.string :as str]
    [reagent.core :as r]
    [tetris.i18n.core :as i18n]
    [tetris.options-store :as options-store]
    [tetris.screens.tip-buttons :refer [tip-buttons-overlay]]))

(defstyles container-class []
  {:max-width "60rem"
   :min-width "28rem"
   :width "50%"})

(defstyles control-item-class []
  {:display "flex"
   :justify-content "space-between"
   :align-items "center"
   :width "100%"
   :line-height "1rem"
   :padding "0.5em 0"
   :border-bottom "1px solid #666"})

(defstyles action-title-class [] {})

(defstyles button-slots-class []
  {:display "flex"})

(defstyles button-slot-class [focused?]
  {:margin-left "1rem"
   :padding "8px 0"
   "&:hover" {:background "rgba(111,111,111,0.1)"}
   :background (if focused? "rgba(111,111,111,0.1)" "transparent")
   :transition "background 0.1s"})

(defstyles button-text-class []
  {:width "4rem"
   :display "inline-block"})

(defstyles bracket-class [focused?]
  {:color (if focused? "#111" "rgba(111,111,111,0.45)")})

(defn button-text [{:keys [button]}]
  (cond
    (str/starts-with? button "Key") (.substr button 3)
    (str/starts-with? button "Arrow") ({"ArrowLeft" "←"
                                      "ArrowRight" "→"
                                      "ArrowUp" "↑"
                                      "ArrowDown" "↓"} button)
    (str/starts-with? button "Control") (str (.substr button 7 1) " Ctrl")
    (str/starts-with? button "Shift") (str (.substr button 5 1) " Shift")
    :else button))

(defn- button-slot [key button focused? on-focus on-select]
  [:div {:key key
         :class (button-slot-class focused?)
         :on-click (if focused? on-select on-focus)}
   [:span {:class (bracket-class focused?)} "〖"]
   [:span {:class (button-text-class)} (and button (button-text button))] 
   [:span {:class (bracket-class focused?)} "〗"]])

(defn- button-receive-overlay []
  [:div
   {:style
    {:z-index 3
     :position "absolute"
     :top "50%"
     :left "50%"
     :transform "translate(-50%, -50%)"
     :background "rgb(0, 0, 0, 0.2)"
     :width "100%"
     :height "100%"
     :text-lign "center"
     :display "flex"
     :flex-direction "column"
     :align-items "center"
     :justify-content "center"}}
   [:p "输入任意想要配置的按键"]
   [:p {:style {:margin-top "8px"}} "按Esc键取消"]])

(def ^:private ordered-actions
  [:move-left
   :move-right
   :rotate-cw
   :rotate-ccw
   :rotate-180
   :soft-drop
   :hard-drop
   :hold])

(defn- change-button [state action slot-index button]
  (assoc state
         :controls (assoc-in (:controls state) [action slot-index] button)
         :button-receiving? false))

(defn- save-controls! [state controls]
  (options-store/save-options! :controls controls)
  (swap! state assoc :controls-unchanged controls))

(defn- reset-controls! [state]
  (let [controls (options-store/slotify-controls
                   (:controls options-store/default-options))]
    (swap! state assoc :controls controls)
    (save-controls! state controls)))

(defn- move-focus! [state dir]
  (if-not (:focused @state)
    (swap! state assoc :focused [(ordered-actions 0) 0])
    (let [[action slot-index] (:focused @state)
          action-index (first (keep-indexed #(when (= %2 action) %1)
                                            ordered-actions))
          [action-index slot-index]
          (case dir
            :left [action-index (max 0 (dec slot-index))]
            :right [action-index (min 2 (inc slot-index))]
            :up [(max (dec action-index) 0) slot-index]
            :down [(min (inc action-index) (dec (count ordered-actions))) slot-index])]
      (swap! state assoc :focused [(ordered-actions action-index) slot-index]))))

(defn- build [{:keys [state]}]
   [:div
    [:h1 (i18n/text [:screen-title :custom-controls])]
    [:div {:class (container-class)}
     (for [action ordered-actions]
       [:div {:key action :class (control-item-class)}
        [:div {:class (action-title-class)}
         (i18n/text [:controls-action action])]
        [:div {:class (button-slots-class)}
         (for [[slot-index button] (map-indexed
                                     vector
                                     (get-in @state [:controls action]))]
           (button-slot [action slot-index]
                        button
                        (= (:focused @state) [action slot-index])
                        #(swap! state assoc :focused [action slot-index])
                        #(swap! state assoc :button-receiving? true)))]])]
    [:div {:style {:margin-top "32px"
                   :visibility (if (and (not (:button-receiving? @state))
                                        (:focused @state)) "visible" "hidden")}}
     "再次点击以配置按键"]
    (when (:button-receiving? @state)
      (button-receive-overlay))
    (tip-buttons-overlay
      ["Del" "清除" {:disabled? (or (not (:focused @state))
                                    (not (get-in @state
                                                 (into [:controls] (:focused @state)))))}]
      ["R" "重置"]
      ["A" "保存" {:disabled? (= (:controls-unchanged @state) (:controls @state))}])])

(defn- on-keydown [{:keys [state]} event]
  (let [key (.-code event)
        button {:type :keyboard :button key}
        [action slot-index] (:focused @state)]
    (cond
      (:button-receiving? @state)
      (if (= key "Escape")
        (swap! state assoc :button-receiving? false)
        (swap! state change-button action slot-index button))

      (:focused @state)
      (case key
        "Enter" (swap! state assoc :button-receiving? true)
        "Delete" (swap! state change-button action slot-index nil)
        "Escape" false
        "KeyA" (save-controls! state (:controls @state))
        "KeyR" (reset-controls! state)
        "ArrowUp" (move-focus! state :up)
        "ArrowRight" (move-focus! state :right)
        "ArrowDown" (move-focus! state :down)
        "ArrowLeft" (move-focus! state :left)
        true)

      (= key "KeyA") (save-controls! state (:controls @state))
      (= key "KeyR") (reset-controls! state)
      (= key "ArrowUp") (move-focus! state :up)
      (= key "ArrowRight") (move-focus! state :right)
      (= key "ArrowDown") (move-focus! state :down)
      (= key "ArrowLeft") (move-focus! state :left)

      :else false)))

(defn make []
  (let [controls (options-store/slotify-controls
                   (options-store/get-options :controls))]
    {:name :controls-options
     :build build
     :on-keydown on-keydown
     :state (r/atom {:controls-unchanged controls
                     :controls controls
                     :focused nil
                     :button-receiving? false})}))

