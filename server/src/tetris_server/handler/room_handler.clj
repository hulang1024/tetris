(ns tetris-server.handler.room-handler 
  (:require
    [tetris-server.handler.room :as room]
    [tetris-server.handler.room-manager :as room-manager]
    [tetris-server.web.api :refer [api-response]]
    [tetris-server.web.ws :as ws]))

(def CreateRoomRequest
  [:map
   [:public :boolean]
   [:players-min :int]
   [:players-max :int]
   [:game-settings [:map]]])

(def JoinRoomRequest
  [:map
   [:room-id :string]])

(def SearchRoomRequest [:map])

(defn search-rooms [{:keys [room-manager]}]
  (api-response :ok (room-manager/get-rooms room-manager)))

(defn create-room [{:keys [room-manager]
                    {:keys [public players-min players-max game-settings]} :json-params
                    {:keys [uid]} :session}]
  (let [create-room {:host uid
                     :public public
                     :players-min players-min
                     :players-max players-max
                     :game-settings game-settings}
        room (or (room-manager/get-joined-room room-manager uid)
                 (room-manager/add-room! room-manager create-room))]
    (api-response :ok {:room room})))

(defn join-room [{:keys [room-manager]
                  {:keys [room-id]} :json-params
                  {:keys [uid]} :session}]
  (let [room (room-manager/get-room room-manager room-id)]
    (cond
      (nil? room) (api-response :not-found)
      (= (:state room) :inprogress) (api-response :failed)
      :else (let [room (room-manager/join-room! room-manager (:id room) uid)
                  joined? (room/joined? room uid)]
              (when joined?
                (ws/broadcast-event! (room/player-uids room) :user-joined {:room room}))
              (api-response (if joined? :ok :failed))))))

(defn leave-room [{:keys [room-manager]
                   {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-found)
      :else (let [room (room-manager/leave-room! room-manager (:id room) uid)]
              (ws/broadcast-event! (room/player-uids room) :user-left {:room room})
              (api-response :ok)))))

(defn toggle-ready [{:keys [room-manager]
                     {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-found)
      :else (let [room (room-manager/toggle-ready! room-manager (:id room) uid)]
              (ws/broadcast-event! (room/player-uids room) :ready-toggled {:room room})
              (api-response :ok)))))

(defn enter-game [{:keys [room-manager]
                   {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-found)
      (not= (:host room) uid) (api-response :failed)
      (not= (:state room) :ready) (api-response :failed)
      :else (let [room (room-manager/enter-game! room-manager (:id room) false)
                  started? (= (:state room) :inprogress)]
              (when started?
                (ws/broadcast-event! (room/player-uids room) :game-entered {:room room}))
              (api-response (if started? :ok :failed))))))

(defn request-start-game [{:keys [room-manager]
                           {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-found)
      (not= (:state room) :inprogress) (api-response :failed)
      :else (let [room (room-manager/request-start-game! room-manager (:id room) uid)]
              (when (room/every-players? room :play-requested)
                (ws/broadcast-event! (room/player-uids room) :game-started {:room room}))
              (api-response :ok)))))
