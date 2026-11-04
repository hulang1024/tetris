(ns tetris.screens.room-create 
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/button" :refer [Button]]
    ["@base-ui/react/input" :refer [Input]]
    [cljss.core :as css :refer-macros [defstyles]]
    [tetris.screens.styles.button :as styles.button]
    [reagent.core :as r]
    [tetris.api :as api]
    [tetris.i18n.core :as i18n]))

(defstyles game-settings-class []
  {:position "fixed"
   :top "50%"
   :left "50%"
   :transform "translate(-50%,-50%)"
   :max-width "18rem"
   :padding "2rem"
   :display "flex"
   :flex-direction "column"
   :justify-content "center"
   :background "#eee"
   :text-align "left"
   "button" {:margin-top "1rem"}})

(defstyles settings-row []
  {:display "flex"
   :align-items "center"
   :margin-bottom "1rem"
   :width "100%"
   ".label" {:min-width "5rem"}
   "input[type=number]" {:padding "0.1rem" :font-size "1rem"}
   "input[type=checkbox]" {:width "1.4rem" :height "1.4rem"}})

(defn- ^:async handle-create-room [room on-done messager]
  (let [resp (await (api/post "/room/create-room" room))]
    (if (= (:code resp) :ok)
      (on-done (:data resp))
      (do (messager "创建失败")
          (on-done nil)))))

(defn room-create-overlay [on-done]
  (r/with-let [state (r/atom {:players-max 3
                              :public true
                              :game-settings {}})]
    [:div {:class (game-settings-class)}
     [:div {:class (settings-row)}
      [:span {:class "label"} (i18n/text [:create-room :players-max])]
      [:> Input {:type "number"
                 :min 2
                 :max 4
                 :autoFocus true
                 :defaultValue (:players-max @state)
                 :on-value-change #(swap! state assoc :players-max %)}]]
     [:div {:class (settings-row)}
      [:span {:class "label"} (i18n/text [:create-room :is-public])]
      [:> Input {:type "checkbox"
                 :checked (:public @state)
                 :on-value-change #(swap! state assoc :public %)}]]

     [:> Button {:class (styles.button/overlay-button {:bg-color :primary})
                 :on-click #(handle-create-room @state on-done js/alert)}
      (i18n/text [:create-room :ok])]
     [:> Button {:class (styles.button/overlay-button)
                 :on-click #(on-done nil)}
      (i18n/text :cancel)]]))
