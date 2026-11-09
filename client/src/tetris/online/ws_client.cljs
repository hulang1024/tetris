(ns tetris.online.ws-client
  (:require [reagent.core :as r]
            [tetris.online.ws-codec :as codec]))

(defonce ^:private state
  (r/atom {:state :offline
           :conn nil
           :event-queue []
           :event-handlers {:player-joined {}
                            :player-left {}
                            :player-ready {}
                            :player-slot-changed {}
                            :room-changed {}
                            :game-started {}}}))

(defn- dispatch-event! [event]
  (let [handlers (get-in @state [:event-handlers (:type event)])]
    (if (seq handlers)
      (doseq [[_k handler] handlers]
        (handler (:data event)))
      (swap! state update :event-queue conj event))))

(defn connect! []
  (let [ws (js/WebSocket. (str "ws://" js/location.host "/api/ws"))]
    (swap! state assoc
           :conn ws
           :state :connecting)
    (set! (.-binaryType ws) "arraybuffer")
    (.addEventListener ws "open"
                       (fn [] (swap! state assoc :state :online)))
    (.addEventListener ws "close"
                       (fn [] (swap! state assoc :state :offline)))
    (.addEventListener ws "message"
                       (fn [event]
                         (dispatch-event! (codec/->server-event (.-data event)))))))

(defn on-api-state-change [api-state]
  (cond
    (and (= api-state :online) (= (:state @state) :offline)) (connect!)
    (= api-state :offline) (when (:conn @state) (.close (:conn @state)))))

(defn subscribe!
  ([event-type handler]
   (subscribe! event-type event-type handler false))
  ([event-type handler take-queue?]
   (subscribe! event-type event-type handler take-queue?))
  ([event-type handler-key handler take-queue?]
   {:pre [(keyword? event-type) (fn? handler)]}
   (swap! state assoc-in [:event-handlers event-type handler-key] handler)
   (when take-queue?
     (let [events (filter #(= (:type %) event-type) (:event-queue @state))]
       (run! dispatch-event! events)))
   (swap! state update :event-queue
          (fn [events] (filter #(not= (:type %) event-type) events)))))

(defn unsubscribe!
  ([event-type]
   (unsubscribe! event-type event-type))
  ([event-type handler-key]
   (swap! state update-in [:event-handlers event-type] dissoc handler-key)))

(defn broadcast-input-frames! [uid input-frames]
  (.send (:conn @state) (codec/encode-input-frames uid input-frames)))
