(ns tetris.screens.screen-container
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.dom.client :as rd]
    [tetris.screens.screen-navigator :as screen-navigator]))

(defonce ^:private root
  (delay (rd/create-root (.getElementById js/document "app"))))

(defstyles screen-class []
  {:display "flex"
   :justify-content "center"
   :flex-direction "column"
   :align-items "center"
   :width "100%"
   :text-align "center"
   :user-select "none"
   "h1" {:z-index 4 :margin-bottom "32px"}})

(defn- add-class-and-key [view class-name key]
  (let [[tag props & children] view]
    (into [tag]
          (if (map? props)
            [(merge {:key key
                     :class (into (flatten [(:class props)]) [class-name])}
                    (dissoc props :class))
             children]
            [{:key key :class class-name} props children]))))

(defn render-screen []
  (when-let [screen (screen-navigator/active-screen)]
    (when-let [view ((:build screen) screen)]
      (add-class-and-key view (screen-class) (name (:name screen))))))

(defn load []
  (.removeEventListener js/window "keydown" screen-navigator/on-keydown)
  (.removeEventListener js/document "visibilitychange" screen-navigator/on-visibilitychange)
  (.addEventListener js/window "keydown" screen-navigator/on-keydown)
  (.addEventListener js/document "visibilitychange" screen-navigator/on-visibilitychange)
  (rd/render @root [render-screen]))
