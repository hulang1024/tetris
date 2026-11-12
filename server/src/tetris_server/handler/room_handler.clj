(ns tetris-server.handler.room-handler 
  (:require
    [tetris-server.handler.room :as room]
    [tetris-server.handler.room-manager :as room-manager]
    [tetris-server.web.api :refer [api-response]]
    [tetris-server.web.ws :as ws]
    [tetris-server.handler.user-manager :as user]))

(def SearchRoomRequest [:map])

(def CreateRoomRequest
  [:map
   [:public :boolean]
   [:players-max :int]])

(def JoinRoomRequest
  [:and
   [:map
    [:room-id {:optional true} :string]
    [:quick-join {:optional true} :boolean]]
   [:fn (fn [{:keys [room-id quick-join]}] (or room-id quick-join))]])

(def ChangeSlotRequest
  [:map
   [:slot [:int {:min 0}]]])

(def ChangeRoleRequest
  [:map
   [:role [:enum "player" "spectator"]]])

(def RequestStartGameRequest
  [:map
   [:seed some?]
   [:handling [:map]]])

(def ChangeRoomSettingsRequest
  [:map
   [:public :boolean]
   [:game-settings [:map]]])

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
               (or (room-manager/get-joined-room room-manager uid)
                   (room-manager/get-match-room room-manager uid))
               (room-manager/get-room room-manager room-id))]
    (if (nil? room)
      (api-response :not-found)
      (let [already-joined? (room/joined? room uid)
            room (room-manager/join-room! room-manager (:id room) uid)
            joined? (room/joined? room uid)]
        (when (and (not already-joined?) joined?)
          (ws/broadcast-event! (room/player-uids room)
                               :player-joined
                               {:room room
                                :player (room/find-player room uid)
                                :user (user/get-user-info datasource {:id uid})}))
        (api-response :ok {:room room
                           :users (user/get-user-infos datasource (room/player-uids room))})))))

(defn leave-room [{:keys [room-manager] {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (if (nil? room)
      (api-response :not-joined)
      (let [uids (room/player-uids room)
            player (room/find-player room uid)
            room (room-manager/leave-room! room-manager (:id room) uid)]
        (ws/broadcast-event! uids :player-left {:room room :player player})
        (api-response :ok)))))

(defn toggle-ready [{:keys [room-manager] {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (if (nil? room)
      (api-response :not-joined)
      (let [room (room-manager/toggle-ready! room-manager (:id room) uid)]
        (ws/broadcast-event! (room/player-uids room)
                             :player-ready
                             {:room room :player (room/find-player room uid)})
        (api-response :ok)))))

(defn change-slot [{:keys [room-manager]
                    {:keys [slot]} :json-params
                    {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (if (nil? room)
      (api-response :not-joined)
      (let [old-slot (:slot (room/find-player room uid))
            room (room-manager/change-slot! room-manager (:id room) uid slot)
            player (room/find-player room uid)
            changed? (and (not= old-slot slot) (= (:slot player) slot))]
        (when changed?
          (ws/broadcast-event! (room/player-uids room)
                               :player-changed
                               {:room room :player player}))
        (api-response (if changed? :ok :failed))))))

(defn change-role [{:keys [room-manager]
                    {:keys [role]} :json-params
                    {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (if (nil? room)
      (api-response :not-joined)
      (let [role (keyword role)
            old-role (:role (room/find-player room uid))
            room (room-manager/change-role! room-manager (:id room) uid role)
            player (room/find-player room uid)
            changed? (and (not= old-role role) (= (:role player) role))]
        (when changed?
          (ws/broadcast-event! (room/player-uids room)
                               :player-changed
                               {:room room :player player}))
        (api-response (if changed? :ok :failed))))))

(defn change-room-settings [{:keys [room-manager]
                        settings :json-params
                        {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (if (nil? room)
      (api-response :not-joined)
      (let [old-room room
            room (room-manager/change-room-settings! room-manager (:id room) settings)
            changed? (not= room old-room)]
        (when changed?
          (ws/broadcast-event! (room/player-uids room) :room-changed {:room room}))
        (api-response (if changed? :ok :failed))))))

(defn enter-game [{:keys [room-manager]
                   {:keys [force-start]} :json-params
                   {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-joined)
      (not= (:host room) uid) (api-response :failed)
      (not= (:state room) :ready) (api-response :failed)
      :else (let [room (room-manager/enter-game! room-manager (:id room) force-start)
                  started? (= (:state room) :inprogress)]
              (when started?
                (ws/broadcast-event! (room/player-uids room) :game-entered {:room room}))
              (api-response (if started? :ok :failed))))))

(defn quit-game [{:keys [room-manager] {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-joined)
      (not= (:state room) :inprogress) (api-response :failed)
      :else (let [room (room-manager/quit-game! room-manager (:id room) uid)]
              (ws/broadcast-event! (room/player-uids room) :player-game-quitted
                                   {:room room
                                    :player (room/find-player room uid)})
              (api-response :ok)))))

(defn request-start-game [{:keys [room-manager datasource]
                           game-config :json-params
                           {:keys [uid]} :session}]
  (let [room (room-manager/get-joined-room room-manager uid)]
    (cond
      (nil? room) (api-response :not-joined)
      (not= (:state room) :inprogress) (api-response :failed)
      :else (let [room (room-manager/request-start-game! room-manager (:id room) uid game-config)]
              (when (room/every-players? room :inprogress)
                (ws/broadcast-event! (room/player-uids room) :game-started
                                     {:room room
                                      :users (user/get-user-infos datasource (room/player-uids room))}))
              (api-response :ok)))))
