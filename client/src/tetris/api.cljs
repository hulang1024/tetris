(ns tetris.api
  (:require [ajax.core :refer [ajax-request json-request-format]]
            [reagent.core :as r]))

(defn- ^:async request [path method params]
  (let [resp
        (await (js/Promise.
                 (fn [resolve reject]
                   (ajax-request
                     {:method method
                      :uri (str "/api" path)
                      :params params
                      :headers {"Content-Type" "application/json"}
                      :handler (comp resolve second)
                      :format (json-request-format)
                      :response-format (ajax.core/json-response-format {:keywords? true})
                      :error-handler reject}))))]
    (update resp :code keyword)))

(defn post [path & [body]]
  (request path :post body))

(defn get! [path query-params]
  (request path :get query-params))

(defonce state
  (r/atom {:api-state :offline ; offline | logging-in | online
           :conn-state :offline
           :user nil
           :conn nil
           :event-handlers {:user-joined {}
                            :user-left {}
                            :ready-toggled {}}}))

(defn ^:async load-user! []
  (let [resp (await (get! "/whoami" nil))]
    (when (= (:code resp) :ok)
      (swap! state assoc
             :api-state :online
             :user (get-in resp [:data :user])))))

(defn connect-server! []
  (let [ws (js/WebSocket. (str "ws://" js/location.host "/api/ws"))]
    (swap! state assoc
           :conn ws
           :conn-state :connecting)
    (.addEventListener ws "open"
                       (fn [] (swap! state assoc :conn-state :online)))
    (.addEventListener ws "close"
                       (fn [] (swap! state assoc :conn-state :offline)))
    (.addEventListener ws "message"
                       (fn [event]
                         (let [event (if (string? event.data)
                                       (js->clj (js/JSON.parse event.data)
                                                :keywordize-keys true)
                                       event.data)
                               event-type (keyword (:type event))]
                           (println (type event-type))
                           (doseq [[_k handler]
                                   (get-in @state [:event-handlers event-type])]
                             (handler event)))))))

(add-watch state :api-state-watcher
           (fn [_key _ref _old-state new-state]
             (cond
               (and (= (:api-state new-state) :online)
                    (= (:conn-state new-state) :offline))
               (connect-server!)

               (= (:api-state new-state) :offline)
               (when (:conn @state) (.close (:conn @state))))))

(defn subscribe-event
  ([event-type handler]
   (subscribe-event event-type event-type handler))
  ([event-type handler-key handler]
   (swap! state assoc-in [:event-handlers event-type handler-key] handler)))

(defn unsubscribe-event
  ([event-type]
   (unsubscribe-event event-type event-type))
  ([event-type handler-key]
   (swap! state update-in [:event-handlers event-type] dissoc handler-key)))
