(ns tetris-server.handler.user-handler
  (:require
    [tetris-server.db.user :as user-db]
    [tetris-server.handler.user-manager :as user]
    [tetris-server.web.api :refer [api-response]]
    [tetris-server.web.ws :as ws]))

(def Username [:string {:min 3}])
(def Password [:string {:min 6}])

(def RegisterRequest
  [:map
   [:username Username]
   [:password Password]])

(def LoginRequest
  [:map
   [:username Username]
   [:password Password]])

(defn- authenticate [ds username password]
  (when-let [user (user-db/get-user ds {:username username})]
    (when (= (:password user) password)
      user)))

(defn register [{:keys [datasource json-params]}]
  (let [{:keys [username password]} json-params]
    (if (user-db/get-user datasource {:username username})
      (api-response :username-exists)
      (do (user-db/save-user! datasource
                              {:username  username
                               :password  password
                               :user-type 1})
          (api-response :ok)))))

(defn login [{:keys [datasource json-params] :as request}]
  (let [{:keys [username password]} json-params
        user (authenticate datasource username password)]
    (if user
      (assoc (api-response :ok {:user (user/safe-user-info user)})
             :session/key (:session/key request)
             :session {:uid (:id user)})
      (api-response :failed))))

(defn logout [{{:keys [uid]} :session}]
  (ws/close! uid)
  (assoc (api-response :ok) :session nil))

(defn whoami [{:keys [datasource] {:keys [uid]} :session}]
  (api-response :ok {:user (user/get-user-info datasource {:id uid})}))

