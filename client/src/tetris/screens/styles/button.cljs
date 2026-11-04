(ns tetris.screens.styles.button
  (:require-macros [cljss.core])
  (:require [cljss.core :refer [defstyles]]))

(def ^:private colors
  {:default "lightblue"
   :primary "cornflowerblue"
   :danger "pink"
   :second "gold"
   :success "lightgreen"})

(defstyles overlay-button [& [{:keys [bg-color full?]}]]
  {:align-self "center"
   :width (when (or full? (nil? full?)) "100%")
   :padding "0.2rem 4rem"
   :font-size "1rem"
   :border-radius "2px"
   :background (get colors bg-color)})
