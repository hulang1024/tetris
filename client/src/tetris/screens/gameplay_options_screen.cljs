(ns tetris.screens.gameplay-options-screen
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/slider" :refer [Slider]]
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.assets :refer [get-piece-styles]]
    [tetris.i18n.core :as i18n]
    [tetris.options-store :as options-store]
    [tetris.screens.tip-buttons :refer [tip-buttons-overlay]]
    [tetris.ui.styles.slider :as slider.styles]))

(defstyles settings-row []
  {:margin-top "2rem"
   :display "flex"
   :flex-direction "column"
   :align-items "center"
   ".label" {:min-width "8rem"}
   "select" {:margin-top "1rem" :flex 1 :padding "0.1rem" :font-size "1rem"}})

(defn- handle-piece-style-change [state value]
  (swap! state assoc :piece-style value)
  (options-store/save-options! :piece-style value))

(defn- option-slider [state label key min max]
  [:> Slider.Root
   {:key (name key)
    :className (slider.styles/root)
    :defaultValue (get-in @state [:handling key])
    :min min
    :max max
    :step 1
    :style {:padding "16px 0"}
    :onValueChange (fn [value]
                     (swap! state assoc-in [:handling key] value)
                     (options-store/save-options! :handling (:handling @state)))}
   [:> Slider.Label {:className (slider.styles/label)} label]
   [:> Slider.Value {:className (slider.styles/value)}]
   [:> Slider.Control {:className (slider.styles/control)}
    [:> Slider.Track {:className (slider.styles/track)}
     [:> Slider.Indicator {:className (slider.styles/indicator)}]
     [:> Slider.Thumb {:className (slider.styles/thumb)}]]]])

(defn make [& [in-game?]]
  {:name :gameplay-options
   :build
   (fn []
     (r/with-let [state (r/atom {:handling (options-store/get-options :handling)
                                 :piece-style (options-store/get-options :piece-style)})]
       [:div {:style {:max-width "60rem"
                      :min-width "20rem"
                      :width "50%"}}
        [:h1 (i18n/text [:screen-title :gameplay])]
        (when (not in-game?)
          (option-slider state "DAS" :das 1 20))
        (when (not in-game?)
          (option-slider state "ARR" :arr 0 6))
        (when (not in-game?)
          (option-slider state "SDF" :sdf 2 40))
        [:div {:class (settings-row)}
         [:div {:class "label"} "方块皮肤"]
         [:select {:value (:piece-style @state)
                   :on-change #(handle-piece-style-change state (.. % -target -value))}
          (map (fn [piece-style] [:option {:value piece-style} piece-style])
               (get-piece-styles))]]
        (tip-buttons-overlay)]))
   :in-game? in-game?})
