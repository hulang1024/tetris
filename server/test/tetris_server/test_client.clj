(ns tetris-server.test-client 
  (:require
    [clj-http.cookies :as cookies]
    [gniazdo.core :as ws-client]
    [tetris-server.api-test-util :as http]))

(defn make-client [username password]
  {:username username
   :password password
   :cs (cookies/cookie-store)})

(defn login [client]
  (http/post "/login"
             (select-keys client [:username :password])
             {:cookie-store (:cs client)}))

(defn logout [client]
  (http/post "/logout" {:cookie-store (:cs client)}))

(defn whoami [client]
  (http/get "/whoami" {:cookie-store (:cs client)}))

(defn search-rooms [client search-room]
  (http/get "/room/search-rooms" search-room {:cookie-store (:cs client)}))

(defn create-room [client create-room]
  (http/post "/room/create-room" create-room {:cookie-store (:cs client)}))

(defn join-room [client room-id]
  (http/post "/room/join-room" {:room-id room-id} {:cookie-store (:cs client)}))

(defn leave-room [client]
  (http/post "/room/leave-room" nil {:cookie-store (:cs client)}))

(defn toggle-ready [client]
  (http/post "/room/toggle-ready" nil {:cookie-store (:cs client)}))

(defn enter-game [client]
  (http/post "/room/enter-game" nil {:cookie-store (:cs client)}))

(defn request-start-game [client]
  (http/post "/room/request-start-game" nil {:cookie-store (:cs client)}))

(defn connect-socket [client]
  (let [uri "ws://localhost:8080/api/ws"
        ws-client (ws-client/client (java.net.URI. uri))]
    (.start ws-client)
    (let [conn (ws-client/connect
                 uri
                 :client ws-client
                 :headers {"Cookie"
                           (cookies/encode-cookies
                             (cookies/get-cookies (:cs client)))}
                 :on-connect (fn [_]
                               (println (str (:username client) " connected")))
                 :on-receive (fn [msg]
                               (println (str (:username client) " received: " msg)))
                 :on-close (fn [_code _reason] (.stop ws-client)))]
      (assoc client :ws-conn conn))))
