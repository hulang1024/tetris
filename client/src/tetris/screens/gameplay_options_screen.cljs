(ns tetris.screens.gameplay-options-screen
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/slider" :refer [Slider]]
    [reagent.core :as r]
    [tetris.i18n.core :as i18n]
    [tetris.options-store :as options-store]
    [tetris.screens.screen :as screen]
    [tetris.screens.styles.slider :as slider.styles]
    [tetris.screens.tip-buttons :refer [tip-buttons-overlay]]))

(defn- option-slider [this label key min max]
  [:> Slider.Root
   {:key (name key)
    :className (slider.styles/root)
    :defaultValue (get-in @this [:handling key])
    :min min
    :max max
    :step 1
    :style {:padding "16px 0"}
    :onValueChange (fn [value]
                     (swap! this assoc-in [:handling key] value)
                     (options-store/save-options! :handling (:handling @this)))}
   [:> Slider.Label {:className (slider.styles/label)} label]
   [:> Slider.Value {:className (slider.styles/value)}]
   [:> Slider.Control {:className (slider.styles/control)}
    [:> Slider.Track {:className (slider.styles/track)}
     [:> Slider.Indicator {:className (slider.styles/indicator)}]
     [:> Slider.Thumb {:className (slider.styles/thumb)}]]]])

(defmethod screen/build :gameplay-options []
  (r/with-let [state (r/atom {:handling (options-store/get-options :handling)})]
    [:div {:style {:max-width "60rem"
                   :min-width "20rem"
                   :width "50%"}}
     [:h1 (i18n/text [:screen-title :gameplay])]
     (option-slider state "DAS" :das 1 20)
     (option-slider state "ARR" :arr 0 6)
     (option-slider state "SDF" :sdf 2 40)
     (tip-buttons-overlay)]))
