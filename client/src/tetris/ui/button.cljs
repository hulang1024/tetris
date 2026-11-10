(ns tetris.ui.button
  (:require-macros [cljss.core])
  (:require [cljss.core :refer [defstyles]]
            ["@base-ui/react/button" :refer [Button]]
            [tetris.audio :as audio]))

(def ^:private colors
  {:default "#fafafa"
   :primary "cornflowerblue"
   :second "gold"
   :danger "pink"
   :success "lightgreen"})

(defstyles button-class [{:keys [bg-color full?]}]
  {:align-self "center"
   :width (when (or full? (nil? full?)) "100%")
   :padding "0.2rem 4rem"
   :font-size "1rem"
   :border-radius "2px"
   :background (get colors (or bg-color :default))})

(defn button [{:keys [on-click] :as props} children]
  [:> Button {:class [(button-class (select-keys props [:bg-color :full?]))
                      (:class props)]
              :style (:style props)
              :disabled (:disabled props)
              :on-click (fn []
                          (when-not (:disabled props)
                            (audio/play :button-click)
                            (on-click)))}
   children])
