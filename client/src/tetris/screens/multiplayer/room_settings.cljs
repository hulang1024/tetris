(ns tetris.screens.multiplayer.room-settings
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/input" :refer [Input]]
    [cljss.core :as css :refer-macros [defstyles]]
    [tetris.i18n.core :as i18n]))

(defstyles overlay-class []
  {:margin-bottom "2rem"
   :min-width "20rem"
   :padding "2rem"
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

(defn room-settings-view [{:keys [public]
                           {:keys [mode rs speed-level preview-count]} :game-settings :as state}
                          host? on-update]
  [:div {:class (overlay-class)}
   (when host?
     [:div {:class (settings-row)}
      [:span {:class "label"} (i18n/text [:create-room :is-public])]
      [:> Input {:type "checkbox"
                 :checked public
                 :disabled (not host?)
                 :on-change #(on-update (assoc state :public (.. % -target -checked)))}]])
   [:div {:class (settings-row)}
    [:span {:class "label"} "模式"]
    [:select {:value mode
              :disabled (not host?)
              :on-change
              (fn [event]
                (let [mode (keyword (.. event -target -value))]
                  (on-update
                    (let [state (assoc-in state [:game-settings :mode] mode)]
                      (if (= mode :classic)
                        (-> state
                            (assoc-in [:game-settings :rs] :nrs-nes)
                            (assoc-in [:game-settings :preview-count] 1))
                        (if (contains? #{:nrs-nes :nrs-game-boy :nrs-dx}
                                       (get-in state [:game-settings :rs]))
                          (assoc-in state [:game-settings :rs] :srs)
                          state))))))}
     [:option {:value :classic} "经典"]
     [:option {:value :modern} "现代"]]]
   [:div {:class (settings-row)}
    [:span {:class "label"} "旋转系统"]
    [:select {:value rs
              :disabled (not host?)
              :on-change #(on-update
                            (assoc-in state [:game-settings :rs] (.. % -target -value)))}
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
               :disabled (not host?)
               :on-value-change #(on-update (assoc-in state [:game-settings :speed-level] (js/parseInt %1)))}]]
   [:div {:class (settings-row)}
    [:span {:class "label"} "预览块数"]
    [:> Input {:type "number"
               :min 0
               :max (if (= mode :classic) 1 5)
               :value preview-count
               :disabled (not host?)
               :on-value-change #(on-update (assoc-in state [:game-settings :preview-count] (js/parseInt %1)))}]]])

