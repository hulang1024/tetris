(ns tetris-server.server
  (:require [camel-snake-kebab.core :as csk]
            [io.pedestal.connector :as connector]
            [io.pedestal.connector.dev :as dev]
            [io.pedestal.environment :as env]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.http-kit :as hk]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.secure-headers :as secure-headers]
            [io.pedestal.http.tracing :as tracing]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.log :as log]
            [io.pedestal.service.interceptors :as interceptors]
            [tetris-server.config :as config]
            [tetris-server.db.common :as db]
            [tetris-server.handler.room-manager :as room-manager]
            [tetris-server.web.routes :as routes]))

(defn inject-handler-deps-interceptor [deps]
  (interceptor
    {:name :inject-deps
     :enter (fn [ctx]
              (update ctx :request merge deps))}))

(defn- log-startup [connector-map]
  (log/info :msg "Service tetris-server startup"
            :port (:port connector-map))
  connector-map)

(defn start
  ([] (start false))
  ([test-mode?]
   (let [config (config/load-config (if test-mode? :test :default))
         pool (db/create-pool (:db config))
         room-manager (room-manager/new-room-manager)
         handler-deps {:room-manager room-manager
                       :datasource (db/create-datasource pool)}]
     (.addShutdownHook (Runtime/getRuntime)
                       (Thread. (fn []
                                  (log/info :msg "Shutting down...")
                                  (.close pool))))
     (-> (connector/default-connector-map (-> config :server :port))
         (assoc :host "0.0.0.0"
                :join? (and (not test-mode?) (not env/dev-mode?)))
         (cond->
           env/dev-mode? (connector/with-interceptors dev/dev-interceptors)
           env/dev-mode? dev/with-interceptor-observer)
         (connector/with-interceptors
           [(tracing/request-tracing-interceptor)
            interceptors/log-request
            interceptors/not-found
            (middlewares/content-type)
            route/query-params
            (body-params/body-params
              (body-params/default-parser-map
                :json-options {:key-fn csk/->kebab-case-keyword}))
            (secure-headers/secure-headers)])
         (connector/with-interceptor (inject-handler-deps-interceptor handler-deps))
         (connector/with-routes (routes/routes))
         log-startup
         (hk/create-connector nil)
         connector/start!))))

(defn stop [server]
  (connector/stop! server))
