(ns tetris.screens.multiplayer.room-join 
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/input" :refer [Input]]
    [cljss.core :as css :refer-macros [defstyles]]
    [clojure.string :as str]
    [reagent.core :as r]
    [tetris.i18n.core :as i18n]
    [tetris.screens.multiplayer.room-manager :as room-manager]
    [tetris.ui.button :refer [button]]))

(defstyles overlay-class []
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

(defstyles input-row []
  {:display "flex"
   :align-items "center"
   :margin-bottom "1rem"
   :width "100%"
   ".label" {:min-width "3rem"}
   "input[type=text]" {:padding "0.1rem" :font-size "1rem"}})

(defn- ^:async handle-join-room [{:keys [room-no]} on-done messager]
  (let [resp (await (room-manager/join-room room-no))]
    (if (= (:code resp) :ok)
      (on-done (:data resp))
      (do (messager (i18n/text [:join-room (:code resp)]))
          (when-not (= (:code resp) :not-found)
            (on-done nil))))))

(defn room-join-dialog [on-done]
  (r/with-let [state (r/atom {:room-no ""})]
    [:div {:class (overlay-class)}
     [:div {:class (input-row)}
      [:> Input {:type "text"
                 :autoFocus true
                 :placeholder (i18n/text [:join-room :room-no])
                 :on-value-change #(swap! state assoc :room-no (str/trim %))}]]
     (button {:bg-color :primary
              :disabled (= (:room-no @state) "")
              :on-click #(handle-join-room @state on-done js/alert)}
             (i18n/text [:join-room :button]))
     (button {:on-click #(on-done nil)}
             (i18n/text :cancel))]))
