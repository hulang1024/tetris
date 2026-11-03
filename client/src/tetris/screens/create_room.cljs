(ns tetris.screens.create-room 
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/button" :refer [Button]]
    ["@base-ui/react/input" :refer [Input]]
    [cljss.core :as css :refer-macros [defstyles]]
    [tetris.api :as api]
    [tetris.i18n.core :as i18n]
    [tetris.screens.screen :as screen]
    [tetris.screens.screen-stack :as screen-stack]))

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
   :text-align "left"})

(defstyles settings-row []
  {:display "flex"
   :align-items "center"
   :margin-bottom "1rem"
   :width "100%"
   ".label" {:min-width "5rem"}
   "input[type=number]" {:padding "0.1rem" :font-size "1rem"}
   "input[type=checkbox]" {:width "1.4rem" :height "1.4rem"}})

(defn- ^:async handle-create-room [room on-success messager]
  (let [res (await (api/post "/room/create-room" room))]
    (if (= (:code res) "ok")
      (on-success)
      (messager "failed"))))

(defmethod screen/init :create-room [_]
  {:players-max 3 :public true})

(defmethod screen/render :create-room [this]
  [:div {:class (game-settings-class)}
   [:div {:class (settings-row)}
    [:span {:class "label"} (i18n/text [:create-room :players-max])]
    [:> Input {:type "number"
               :autoFocus true
               :defaultValue (:players-max @this)
               :on-value-change #(swap! this assoc :players-max %)}]]
   [:div {:class (settings-row)}
    [:span {:class "label"} (i18n/text [:create-room :is-public])]
    [:> Input {:type "checkbox"
               :checked (:public @this)
               :on-value-change #(swap! this assoc :public %)}]]

   [:> Button {:style {:margin-top "1rem"
                       :align-self "center"
                       :padding "0.2rem 4rem"
                       :font-size "1rem"}
               :on-click (fn []
                           (handle-create-room
                             this
                             #(screen-stack/push-screen! :room-screen)
                             js/alert))}
    (i18n/text [:create-room :ok])]])
