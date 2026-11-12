(ns tetris.online.api
  (:require [ajax.core :refer [ajax-request json-request-format]]
            [reagent.core :as r]
            [tetris.online.ws-client :as ws-client]
            [tetris.options-store :as options-store]))

(def State [:enum :offline :loggin-in :online])

(defonce ^:private state
  (r/atom {:state :offline
           :user nil}))

(add-watch state :api-state-watcher
           (fn [_key _ref _old-state new-state]
             (ws-client/on-api-state-change (:state new-state))))

(defn get-state
  {:malli/schema [:=> [:cat] State]}
  [] (:state @state))

(defn local-user [] (:user @state))

(declare try-auto-login)

(defn- handle-response [resolve [ok? resp]]
  (if ok?
    (resolve resp)
    (when (>= (:status resp) 401)
      (swap! state assoc :state :offline :user nil)
      (js/setTimeout try-auto-login 3000)
      (resolve resp))))

(defn- ^:async request [path method params]
  (let [resp
        (await (js/Promise.
                 (fn [resolve reject]
                   (ajax-request
                     {:method method
                      :uri (str "/api" path)
                      :params params
                      :headers {"Content-Type" "application/json"}
                      :handler #(handle-response resolve %)
                      :format (json-request-format)
                      :response-format (ajax.core/json-response-format {:keywords? true})
                      :error-handler reject}))))]
    (update resp :code keyword)))

(defn post [path & [body]]
  (request path :post body))

(defn get! [path query-params]
  (request path :get query-params))

(defn ^:async login! [username password]
  (swap! state assoc :state :loggin-in)
  (let [resp (await (post "/login" {:username username :password password}))]
    (if (= (:code resp) :ok)
      (swap! state assoc :state :online :user (get-in resp [:data :user]))
      (swap! state assoc :state :offline))
    resp))

(defn register! [username password]
  (post "/register" {:username username :password password}))

(defn get-online-stats []
  (get! "/online-stats" nil))

(defn- ^:async try-auto-login []
  (let [{:keys [auto-login-enabled username password]} (options-store/get-options)]
    (when (and auto-login-enabled username password)
      (let [resp (await (login! username password))]
        (when (and (some? (:code resp)) (not= (:code resp) :ok))
          (options-store/save-options! :auto-login-enabled false))))))

(defn ^:async load-session-user! []
  (let [resp (await (get! "/whoami" nil))]
    (if (= (:code resp) :ok)
      (swap! state assoc
             :state :online
             :user (get-in resp [:data :user]))
      (try-auto-login))))
