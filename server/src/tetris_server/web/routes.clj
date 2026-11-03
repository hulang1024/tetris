(ns tetris-server.web.routes
  (:require [io.pedestal.http.ring-middlewares :as middlewares]
            [io.pedestal.http.route.definition.table :as table]
            [io.pedestal.interceptor :refer [interceptor]]
            [malli.core :as m]
            [malli.error :as me]
            [tetris-server.handler.room-handler :as room-handler]
            [tetris-server.handler.user-handler :as user-handler]
            [tetris-server.web.api :refer [api-response]]
            [tetris-server.web.ws :refer [ws-interceptor]]))

(defn validation-params [schema]
  (interceptor
    {:name ::validate-params
     :enter (fn [ctx]
              (let [params (merge (get-in ctx [:request :json-params])
                                  (get-in ctx [:request :query-params]))
                    err  (and (seq params) (m/explain schema params))]
                (if err
                  (assoc ctx :response
                         (api-response :error {:errors (me/humanize err)}))
                  ctx)))}))

(def auth-interceptor
  (interceptor
    {:name ::auth
     :enter (fn [ctx]
              (let [{:keys [uid]} (get-in ctx [:request :session])]
                (if uid
                  ctx
                  (assoc ctx :response {:status 401}))))}))

(def extract-session-id-interceptor
  (interceptor
    {:name :extract-session-id 
     :enter (fn [ctx]
              (if (get-in ctx [:request :cookies "ring-session"])
                ctx
                (if-let [sid (get-in ctx [:request :headers "x-session-id"])]
                  (-> ctx
                      (assoc-in [:request :cookies "ring-session"] {:value sid})
                      (assoc-in [:request :session/key] sid))
                  ctx)))}))

(defonce session-interceptor (middlewares/session))

(defn api-route [verb path handler params-schema & [opts]]
  [(str "/api" path)
   verb (cond-> []
          true (into [extract-session-id-interceptor session-interceptor])
          (not= (:auth-required? opts) false)
          (conj auth-interceptor)
          (some? params-schema) (conj (validation-params params-schema))
          true (conj handler))
   :route-name (or (:route-name opts)
                   (keyword (subs path 1)))])

(defn routes []
  (table/table-routes
    [(api-route :post "/register" user-handler/register user-handler/RegisterRequest {:auth-required? false})
     (api-route :post "/login" user-handler/login user-handler/LoginRequest {:auth-required? false})
     (api-route :post "/logout" user-handler/logout nil)
     (api-route :get "/whoami" user-handler/whoami nil)

     (api-route :get "/ws" ws-interceptor nil)

     (api-route :get "/room/search-rooms" room-handler/search-rooms room-handler/SearchRoomRequest)
     (api-route :post "/room/create-room" room-handler/create-room room-handler/CreateRoomRequest)
     (api-route :post "/room/join-room" room-handler/join-room room-handler/JoinRoomRequest)
     (api-route :post "/room/leave-room" room-handler/leave-room nil)
     (api-route :post "/room/toggle-ready" room-handler/toggle-ready nil)
     (api-route :post "/room/enter-game" room-handler/enter-game nil)
     (api-route :post "/room/request-start-game" room-handler/request-start-game nil)]))
