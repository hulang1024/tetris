(ns tetris-server.handler.room-handler 
  (:require
    [tetris-server.handler.room :as room]
    [tetris-server.handler.room-manager :as room-manager]
    [tetris-server.web.api :refer [api-response]]
    [tetris-server.web.ws :as ws]
    [tetris-server.handler.user-manager :as user]))

(def CreateRoomRequest
  [:map
   [:public :boolean]
   [:players-max :int]
   [:game-settings [:map]]])

(def JoinRoomRequest
  [:and
   [:map
    [:room-id {:optional true} :string]
    [:quick-join {:optional true} :boolean]]
   [:fn (fn [{:keys [room-id quick-join]}] (or room-id quick-join))]])

(def ChangeSlotRequest
  [:map
   [:slot [:int {:min 0}]]])

(def RequestStartGameRequest
  [:map
   [:seed some?]
   [:handling [:map]]])

(def SearchRoomRequest [:map])

(defn search-rooms [{:keys [room-manager]}]
  (api-response :ok (room-manager/get-rooms room-manager)))

(defn create-room [{:keys [room-manager datasource]
                    {:keys [public players-max game-settings]} :json-params
                    {:keys [uid]} :session}]
  (let [create-room {:id ""
                     :host uid
                     :public public
                     :players-min 2
                     :players-max players-max
                     :game-settings game-settings}
        room (or (room-manager/get-joined-room room-manager uid)
                 (room-manager/add-room! room-manager create-room))]
    (api-response :ok {:room room
                       :users (user/get-user-infos datasource (room/player-uids room))})))

(defn join-room [{:keys [room-manager datasource]
                  {:keys [room-id quick-join]} :json-params
                  {:keys [uid]} :session}]
  (let [room (if quick-join
               (room-manager/get-match-room room-manager uid)
               (room-manager/get-room room-manager room-id))]
    (cond
      (nil? room) (api-response :not-found)
      (= (:state room) :inprogress) (api-response :failed)
      :else (let [room (room-manager/join-room! room-manager (:id room) uid)
                  joined? (room/joined? room uid)]
              (when joined?
                (ws/broadcast-event! (room/player-uids room)
                                     :player-joined
                                     {:room room
                                      :player (room/find-player room uid)
                                      :user (user/get-user-info datasource {:id uid})}))
              (api-response (if joined? :ok :failed)
                            {:room room
                             :users (user/get-user-infos datasource (room/player-uids room))})))))

(defn leave-room [{:keys [room-manager] {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-found)
      :else (let [uids (room/player-uids room)
                  player (room/find-player room uid)
                  room (room-manager/leave-room! room-manager (:id room) uid)]
              (ws/broadcast-event! uids :player-left {:room room :player player})
              (api-response :ok)))))

(defn toggle-ready [{:keys [room-manager] {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-found)
      :else (let [room (room-manager/toggle-ready! room-manager (:id room) uid)]
              (ws/broadcast-event! (room/player-uids room)
                                   :player-ready
                                   {:room room :player (room/find-player room uid)})
              (api-response :ok)))))

(defn change-slot [{:keys [room-manager]
                    {:keys [slot]} :json-params
                    {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-found)
      :else (let [old-slot (:slot (room/find-player room uid))
                  room (room-manager/change-slot! room-manager (:id room) uid slot)
                  player (room/find-player room uid)
                  changed? (and (not= old-slot slot) (= (:slot player) slot))]
              (when changed?
                (ws/broadcast-event! (room/player-uids room)
                                     :player-slot-changed
                                     {:room room :player player}))
              (api-response (if changed? :ok :failed))))))

(defn enter-game [{:keys [room-manager] {:keys [uid]} :session}]
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

(defn request-start-game [{:keys [room-manager datasource]
                           game-config :json-params
                           {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-found)
      (not= (:state room) :inprogress) (api-response :failed)
      :else (let [room (room-manager/request-start-game! room-manager (:id room) uid game-config)]
              (when (room/every-players? room :play-requested)
                (ws/broadcast-event! (room/player-uids room) :game-started
                                     {:room room
                                      :users (user/get-user-infos datasource (room/player-uids room))}))
              (api-response :ok)))))
