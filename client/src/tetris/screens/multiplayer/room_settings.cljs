(ns tetris.screens.multiplayer.room-settings
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/input" :refer [Input]]
    [cljss.core :as css :refer-macros [defstyles]]
    [tetris.i18n.core :as i18n]
    [tetris.screens.custom-game :refer [custom-game-panel]]))

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
   "input[type=checkbox]" {:width "1.4rem" :height "1.4rem"}})

(defn room-settings-view [{:keys [public game-settings] :as state} host? on-update]
  [:div {:class (overlay-class)}
   (when host?
     [:div {:class (settings-row)}
      [:span {:class "label"} (i18n/text [:create-room :is-public])]
      [:> Input {:type "checkbox"
                 :checked public
                 :disabled (not host?)
                 :on-change #(on-update (assoc state :public (.. % -target -checked)))}]])
   (custom-game-panel
     game-settings
     (not host?)
     (fn [game-settings]
       (on-update (assoc state :game-settings game-settings))))])

