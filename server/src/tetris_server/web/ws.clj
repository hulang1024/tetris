(ns tetris-server.web.ws
  (:require [cheshire.core :as json]
            [io.pedestal.service.websocket :as ws]
            [tetris-server.handler.room :as room]
            [tetris-server.handler.room-manager :as room-manager]))

(defonce ^:private ws-channels (atom {}))

(defn- add! [uid ch]
  (swap! ws-channels assoc uid ch))

(defn- del! [uid]
  (swap! ws-channels dissoc uid))

(defn close! [uid]
  (when-let [ch (get @ws-channels uid)]
    (ws/close! ch)))

(defn broadcast-event! [uids event-type data]
  (doseq [ch (map #(get @ws-channels %) uids)
          :when (some? ch)
          :let [text (json/generate-string {:type event-type :data data})]]
    (ws/send-text! ch text)))

(defn- broadcast-binary! [uids bin]
  (doseq [ch (map #(get @ws-channels %) uids)
          :when (some? ch)]
    (ws/send-binary! ch bin)))

(def ws-interceptor
  (ws/websocket-interceptor
    ::game
    {:on-open  (fn [channel request]                        
                 (assert (some? (:session request)))
                 (add! (-> request :session :uid) channel)
                 request)
     :on-text  (fn [channel _ msg-json]
                 (when-let [msg (json/parse-string msg-json true)]
                   (when (= (:type msg) "stat-online-users")
                     (ws/send-text! channel (str (count @ws-channels))))))
     :on-brinary (fn [_channel {:keys [room-manager] {:keys [uid]} :session} byte-buffer]
                   (let [room (room-manager/get-joined-room room-manager uid)]
                     (when room
                       (broadcast-binary!
                         (remove #(= % uid) (room/player-uids room))
                         (.array byte-buffer)))))
     :on-close (fn [_channel {:keys [room-manager] {:keys [uid]} :session} _reason]                    
                 (del! uid)
                 (when-let [joined-room (room-manager/get-joined-room room-manager uid)]
                   (let [room (room-manager/leave-room! room-manager (:id joined-room) uid)]
                     (broadcast-event! (room/player-uids room)
                                       :player-left
                                       {:room room :player (room/find-player room uid)}))))}))
