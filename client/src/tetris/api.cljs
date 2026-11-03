(ns tetris.api
  (:require [ajax.core :refer [ajax-request]]
            [tetris.global :refer [app]]))

(defn- request [path method params]
  (js/Promise.
    (fn [resolve reject]
      (ajax-request
        {:method method
         :uri (str "/api" path)
         :params params
         :headers {"Content-Type" "application/json"}
         :handler (comp resolve second)
         :response-format (ajax.core/json-response-format {:keywords? true})
         :error-handler reject}))))

(defn post [path body]
  (request path :post (js/JSON.stringify (clj->js body))))

(defn get! [path query-params]
  (request path :get (clj->js query-params)))

(defn ^:async load-user! []
  (let [resp (await (get! "/whoami" nil))]
    (when (= (:code resp) "ok")
      (swap! app assoc
             :online-state :online
             :user (get-in resp [:data :user])))))
