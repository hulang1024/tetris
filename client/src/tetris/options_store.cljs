(ns tetris.options-store 
  (:require [tetris.options :as options :refer [Controls Handling Options]]))

(def ^:private storage-key "tetris")

(def Category [:enum :handling :controls :piece-style :username :password :auto-login-enabled])

(defn- read-options []
  (let [object (js/JSON.parse (js/localStorage.getItem storage-key))]
    (if object
      (-> (js->clj object {:keywordize-keys true})
          (update :controls
                  (fn [cs]
                    (update-vals 
                      cs
                      (fn [v]
                        (mapv #(when % (update % :type keyword)) v))))))
      options/default-options)))

(def options (atom (read-options)))

(defn get-options
  {:malli/schema [:function
                  [:=> [:cat] Options]
                  [:=> [:cat Category] [:or Handling Controls :nil :string :boolean]]]}
  ([] (read-options))
  ([category] (get (read-options) category (get options/default-options category))))

(defn save-options!
  {:malli/schema [:=> [:cat Category [:or Handling Controls :string :boolean]] Options]}
  [category sub-options]
  (let [opts (assoc (get-options) category sub-options)
        json (js/JSON.stringify (clj->js opts))]
    (js/localStorage.setItem storage-key json)
    (reset! options opts)))
