(ns tetris.online.ws-client
  (:require [reagent.core :as r]))

(defonce ^:private state
  (r/atom {:state :offline
           :conn nil
           :event-handlers {:player-joined {}
                            :player-left {}
                            :player-ready {}}}))

(defn- dispatch-message! [raw-data]
  (let [event (if (string? raw-data)
                (js->clj (js/JSON.parse raw-data)
                         :keywordize-keys true)
                raw-data)
        event-type (keyword (:type event))]
    (doseq [[_k handler]
            (get-in @state [:event-handlers event-type])]
      (handler event))))

(defn connect! []
  (let [ws (js/WebSocket. (str "ws://" js/location.host "/api/ws"))]
    (swap! state assoc
           :conn ws
           :state :connecting)
    (.addEventListener ws "open"
                       (fn [] (swap! state assoc :state :online)))
    (.addEventListener ws "close"
                       (fn [] (swap! state assoc :state :offline)))
    (.addEventListener ws "message"
                       (fn [event] (dispatch-message! (.-data event))))))

(defn on-api-state-change [api-state]
  (cond
    (and (= api-state :online) (= (:state @state) :offline)) (connect!)
    (= api-state :offline) (when (:conn @state) (.close (:conn @state)))))

(defn subscribe!
  ([event-type handler]
   (subscribe! event-type event-type handler))
  ([event-type handler-key handler]
   (swap! state assoc-in [:event-handlers event-type handler-key] handler)))

(defn unsubscribe!
  ([event-type]
   (unsubscribe! event-type event-type))
  ([event-type handler-key]
   (swap! state update-in [:event-handlers event-type] dissoc handler-key)))
