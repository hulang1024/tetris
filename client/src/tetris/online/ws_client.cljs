(ns tetris.online.ws-client
  (:require [reagent.core :as r]
            [tetris.online.ws-codec :as codec]))

(defonce ^:private state
  (r/atom {:state :offline
           :api-online? false
           :conn nil
           :event-queue []
           :event-handlers {:connected {}
                            :disconnected {}
                            :player-joined {}
                            :player-left {}
                            :player-ready {}
                            :player-changed {}
                            :player-game-quitted {}
                            :room-changed {}
                            :game-started {}}}))

(defn- dispatch-event! [event]
  (let [handlers (get-in @state [:event-handlers (:type event)])]
    (if (seq handlers)
      (doseq [[_group-key handler] handlers]
        (handler (:data event)))
      (swap! state update :event-queue conj event))))

(defn connect! []
  (println "connecting...")
  (let [ws (js/WebSocket. (str "ws://" js/location.host "/api/ws"))]
    (swap! state assoc
           :conn ws
           :state :connecting)
    (set! (.-binaryType ws) "arraybuffer")
    (.addEventListener ws "open"
                       (fn []
                         (println "connected")
                         (swap! state assoc :state :online)
                         (dispatch-event! {:type :connected :data nil})))
    (.addEventListener ws "close"
                       (fn []
                         (println "disconnected")
                         (swap! state assoc :state :offline)
                         (dispatch-event! {:type :disconnectd :data nil})
                         (when (:api-online? @state)
                           (js/setTimeout connect! 3000))))
    (.addEventListener ws "message"
                       (fn [event]
                         (dispatch-event! (codec/->server-event (.-data event)))))))

(defn on-api-state-change [api-state]
  (swap! state assoc :api-online? (= api-state :online))
  (if (= api-state :online) 
    (when (= (:state @state) :offline)
      (connect!))
    (when (and (some? (:conn @state))
               (not= (.-readyState (:conn @state)) js/WebSocket.OPEN))
      (.close (:conn @state)))))

(defn subscribe!
  ([event-type handler]
   (subscribe! :default event-type handler))
  ([group-key event-type handler]
   (subscribe! group-key event-type handler false))
  ([group-key event-type handler take-queue?]
   {:pre [(keyword? event-type) (fn? handler)]}
   (swap! state assoc-in [:event-handlers event-type group-key] handler)
   (when take-queue?
     (let [events (filter #(= (:type %) event-type) (:event-queue @state))]
       (run! dispatch-event! events)))
   (swap! state update :event-queue
          (fn [events] (filter #(not= (:type %) event-type) events)))))

(defn unsubscribe!
  ([event-type]
   (unsubscribe! :default event-type))
  ([group-key event-type]
   (if (= event-type :all)
     (swap! state update :event-handlers
            (fn [event-handlers]
              (reduce-kv (fn [m event-type handlers]
                           (assoc m event-type (dissoc handlers group-key)))
                         {}
                         event-handlers) {}))
     (swap! state update-in [:event-handlers event-type] dissoc group-key))))

(defn broadcast-input-frames! [uid input-frames]
  (when (= (.-readyState (:conn @state)) js/WebSocket.OPEN)
    (.send (:conn @state) (codec/encode-input-frames uid input-frames))))
