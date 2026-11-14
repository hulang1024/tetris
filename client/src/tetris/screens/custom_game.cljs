(ns tetris.screens.custom-game
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/input" :refer [Input]]
    [cljss.core :as css :refer-macros [defstyles]]))

(defstyles view-class []
  {:min-width "20rem"
   :display "flex"
   :flex-direction "column"
   :text-align "left"})

(defstyles settings-row []
  {:display "flex"
   :align-items "center"
   :margin-bottom "1rem"
   ".label" {:min-width "8rem"}
   "input[type=number],select" {:flex 1 :padding "0.1rem" :font-size "1rem"}
   "input[type=checkbox]" {:width "1.4rem" :height "1.4rem"}})

(defn custom-game-panel [{:keys [mode rs speed-level preview-count] :as state} readonly? on-update & [auto-focus?]]
  [:div {:class (view-class)}
   [:div {:class (settings-row)}
    [:span {:class "label"} "模式"]
    [:select {:value mode
              :disabled readonly?
              :on-change
              (fn [event]
                (let [mode (keyword (.. event -target -value))]
                  (on-update
                    (let [state (assoc state :mode mode)]
                      (if (= mode :classic)
                        (-> state
                            (assoc :rs :nrs-nes)
                            (assoc :preview-count 1))
                        (if (contains? #{:nrs-nes :nrs-game-boy :nrs-dx} (:rs state))
                          (assoc state :rs :srs)
                          state))))))}
     [:option {:value :classic} "经典"]
     [:option {:value :modern} "现代"]]]
   [:div {:class (settings-row)}
    [:span {:class "label"} "旋转系统"]
    [:select {:value rs
              :disabled readonly?
              :on-change #(on-update
                            (assoc state :rs (.. % -target -value)))}
     [:option {:value :srs :disabled (= mode :classic)} "SRS"]
     [:option {:value :prs :disabled (= mode :classic)} "PRS"]
     [:option {:value :nrs-nes :disabled (not= mode :classic)} "NRS NES"]
     [:option {:value :nrs-game-boy :disabled (not= mode :classic)} "NRS Game boy"]
     [:option {:value :nrs-dx :disabled (not= mode :classic)} "NES DX"]]]
   [:div {:class (settings-row)}
    [:span {:class "label"} "初始速度等级"]
    [:> Input {:type "number"
               :min ({:classic 0 :modern 1} mode)
               :value speed-level
               :autoFocus auto-focus?
               :disabled readonly?
               :on-value-change #(on-update (assoc state :speed-level (js/parseInt %1)))}]]
   [:div {:class (settings-row)}
    [:span {:class "label"} "预览块数"]
    [:> Input {:type "number"
               :min 0
               :max (if (= mode :classic) 1 5)
               :value preview-count
               :disabled readonly?
               :on-value-change #(on-update (assoc state :preview-count (js/parseInt %1)))}]]])

