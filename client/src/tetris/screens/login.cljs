(ns tetris.screens.login
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/input" :refer [Input]]
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.i18n.core :as i18n]
    [tetris.online.api :as api]
    [tetris.ui.button :refer [button]]
    [tetris.options-store :as options-store]))

(defstyles logic-overlay-class []
  {:position "fixed"
   :top "50%"
   :left "50%"
   :transform "translate(-50%,-50%)"
   :min-width "18rem"
   :padding "2rem"
   :display "flex"
   :flex-direction "column"
   :justify-content "center"
   :background "#eee"
   :text-align "left"
   :border-radius "1px"})

(defstyles input-item-class [& [row?]]
  {:margin-bottom "1rem"
   :width "100%"
   :display "flex"
   :flex-direction (if row? "row" "column")
   :align-items (when row? "center")
   "label" {:margin-bottom (if row? 0 "0.2rem")}
   "input" {:margin-top "0.4rem"
            :padding "0.2rem"
            :font-size "1rem"}
   "input[type=checkbox]" {:width "1.4rem" :height "1.4rem"}})

(defstyles message-class []
  {:margin-bottom "1rem"
   :min-height "1rem"
   :color "orangered"})

(defstyles button-class []
  {:margin-top "0.5rem"})

(defstyles title-class []
  {:margin-bottom "2rem"
   :font-size "1.3rem"})

(defn- ^:async handle-login [{:keys [username password auto-login-enabled]} on-success messager]
  (let [resp (await (api/login! username password))]
    (if (= (:code resp) :ok)
      (do (options-store/save-options! :username username)
          (when auto-login-enabled
            (options-store/save-options! :password password))
          (on-success))
      (messager
        (if (= (:code resp) :error)
          (i18n/format [:login-message :format-error] {:u 3 :p 6})
          (i18n/text [:login-message :failed]))))))

(defn- ^:async handle-register [{:keys [username password]} on-success messager]
  (let [resp (await (api/register! username password))]
    (if (= (:code resp) :ok)
      (on-success (:data resp))
      (messager
        (if (= (:code resp) :error)
          (i18n/format [:register-message :format-error] {:u 3 :p 6})
          (i18n/text [:register-message (:code resp)]))))))

(defn logic-overlay [on-done]
  (r/with-let [state (r/atom {:username (options-store/get-options :username)
                              :password (options-store/get-options :password)
                              :auto-login-enabled (options-store/get-options :auto-login-enabled) 
                              :mode :login
                              :message nil})
               set-message #(swap! state assoc :message %)]
    (let [{:keys [username password auto-login-enabled mode message]} @state]
      [:div {:class (logic-overlay-class)}
       [:span {:class (title-class)}
        (if (= mode :login) "登录" "注册")]
       [:div {:class (input-item-class)}
        [:label (i18n/text [:login :username])]
        [:> Input {:type "text"
                   :value username
                   :autoFocus (not username)
                   :on-value-change #(swap! state assoc :username %)}]]
       [:div {:class (input-item-class)}
        [:label (i18n/text [:login :password])]
        [:> Input {:type "password"
                   :value password
                   :autoFocus (some? username)
                   :on-value-change #(swap! state assoc :password %)}]]
       (when (= mode :login)
         [:div {:class (input-item-class true)}
          [:label {:class "label"} (i18n/text [:login :auto-login])]
          [:> Input {:type "checkbox"
                     :checked auto-login-enabled
                     :on-change (fn [event]
                                  (let [on? (.. event -target -checked)]
                                    (swap! state assoc :auto-login-enabled on?)
                                    (options-store/save-options! :auto-login-enabled on?)))}]])
       [:span {:class (message-class)} message]
       (when (= mode :login)
         (button {:class (button-class)
                  :bg-color :primary
                  :on-click (fn []
                              (set-message nil)
                              (handle-login @state on-done set-message))}
                 (i18n/text [:login :button])))
       (when (= mode :login)
         (button {:class (button-class)
                  :bg-color :second
                  :on-click #(swap! state assoc :mode :register :password "")}
                 (i18n/text [:register :button])))
       (when (= mode :register)
         (button {:class (button-class)
                  :bg-color :primary
                  :on-click (fn []
                              (set-message nil)
                              (handle-register @state
                                               #(swap! state assoc :mode :login :password "")
                                               set-message))}
                 (i18n/text :ok)))
       (button {:class (button-class)
                :on-click #(if (= mode :login)
                             (on-done)
                             (swap! state assoc :mode :login))}
               (i18n/text :cancel))])))

