(ns tetris.online.api
  (:require [ajax.core :refer [ajax-request json-request-format]]
            [reagent.core :as r]
            [tetris.online.ws-client :as ws-client]))

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

(defn- ^:async request [path method params]
  (let [resp
        (await (js/Promise.
                 (fn [resolve reject]
                   (ajax-request
                     {:method method
                      :uri (str "/api" path)
                      :params params
                      :headers {"Content-Type" "application/json"}
                      :handler (comp resolve second)
                      :format (json-request-format)
                      :response-format (ajax.core/json-response-format {:keywords? true})
                      :error-handler reject}))))]
    (update resp :code keyword)))

(defn post [path & [body]]
  (request path :post body))

(defn get! [path query-params]
  (request path :get query-params))

(defn ^:async login! [user]
  (swap! state assoc :state :loggin-in)
  (let [resp (await (post "/login" user))]
    (if (= (:code resp) :ok)
      (swap! state assoc :state :online :user (get-in resp [:data :user]))
      (swap! state assoc :state :offline))
    resp))

(defn register! [user]
  (post "/register" user))

(defn ^:async load-session-user! []
  (let [resp (await (get! "/whoami" nil))]
    (when (= (:code resp) :ok)
      (swap! state assoc
             :state :online
             :user (get-in resp [:data :user])))))
