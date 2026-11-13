(ns tetris.screens.solo-custom-game
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.i18n.core :as i18n]
    [tetris.ui.button :refer [button]]
    [tetris.screens.custom-game :refer [custom-game-panel]]))

(defstyles overlay-class []
  {:position "fixed"
   :top "50%"
   :left "50%"
   :transform "translate(-50%,-50%)"
   :max-width "24rem"
   :padding "2rem"
   :display "flex"
   :flex-direction "column"
   :justify-content "center"
   :background "#eee"
   :text-align "left"
   "button" {:margin-top "0.5rem"}})

(defn solo-custom-game-dialog [on-done]
  (r/with-let [state (r/atom {:mode :modern
                              :speed-level 1
                              :rs :srs
                              :preview-count 5})]
    [:div {:class (overlay-class)}
     (custom-game-panel @state false #(reset! state %))
     (button {:bg-color :primary
              :on-click #(on-done @state)}
             (i18n/text :ok))
     (button {:on-click #(on-done nil)}
             (i18n/text :cancel))]))
