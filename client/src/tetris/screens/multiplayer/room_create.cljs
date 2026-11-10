(ns tetris.screens.multiplayer.room-create 
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/input" :refer [Input]]
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.i18n.core :as i18n]
    [tetris.online.room-api :as room-api]
    [tetris.ui.button :refer [button]]))

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

(defstyles settings-row []
  {:display "flex"
   :align-items "center"
   :margin-bottom "1rem"
   :width "100%"
   ".label" {:min-width "8rem"}
   "input[type=number],select" {:width "6rem" :padding "0.1rem" :font-size "1rem"}
   "input[type=checkbox]" {:width "1.4rem" :height "1.4rem"}})

(defn- ^:async handle-create-room [room on-done messager]
  (let [resp (await (room-api/create-room room))]
    (if (= (:code resp) :ok)
      (on-done (:data resp))
      (do (messager "创建失败")
          (on-done nil)))))

(defn room-create-dialog [on-done]
  (r/with-let [state (r/atom {:players-max 2
                              :public true
                              :game-settings {:mode :modern
                                              :speed-level 1
                                              :rs :srs
                                              :preview-count 5}})]
    [:div {:class (overlay-class)}
     [:div {:class (settings-row)}
      [:span {:class "label"} (i18n/text [:create-room :players-max])]
      [:> Input {:type "number"
                 :min 2
                 :max 4
                 :autoFocus true
                 :defaultValue (:players-max @state)
                 :on-value-change #(swap! state assoc :players-max (js/parseInt %1))}]]
     [:div {:class (settings-row)}
      [:span {:class "label"} (i18n/text [:create-room :is-public])]
      [:> Input {:type "checkbox"
                 :checked (:public @state)
                 :on-change #(swap! state assoc :public (.. % -target -checked))}]]
     (button {:bg-color :primary
              :on-click #(handle-create-room @state on-done js/alert)}
             (i18n/text [:create-room :ok]))
     (button {:on-click #(on-done nil)}
             (i18n/text :cancel))]))
