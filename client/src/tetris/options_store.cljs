(ns tetris.options-store 
  (:require [tetris.options :as options :refer [Controls Handling Options]]))

(defn- read-options []
  (let [object (js/JSON.parse (js/localStorage.getItem "tetris"))]
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
                  [:=> [:cat [:enum :handling :controls]] [:or Handling Controls]]]}
  ([] (read-options))
  ([category] (get (read-options) category)))

(defn save-options!
  {:malli/schema [:=> [:cat [:enum :handling :controls]
                       [:or Handling Controls]] Options]}
  [category sub-options]
  (let [opts (assoc (get-options) category sub-options)
        json (js/JSON.stringify (clj->js opts))]
    (js/localStorage.setItem "tetris" json)
    (reset! options opts)))
