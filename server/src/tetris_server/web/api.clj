(ns tetris-server.web.api
  (:require [cheshire.core :as json]))

(defn api-response
  ([code] (api-response code nil))
  ([code data]
   {:status 200
    :headers {"Content-Type" "application/json"}
    :body  (json/generate-string (cond-> {:code code}
                                   data (assoc :data data)))}))

