(ns tetris.screens.screen-container
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.dom.client :as rd]
    [tetris.screens.screen-stack :as screen-stack]))

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
  (when-let [screen (screen-stack/active-screen)]
    (when-let [view ((:build screen) screen)]
      (add-class-and-key view (screen-class) (name (:name screen))))))

(defn load []
  (.removeEventListener js/window "keydown" screen-stack/on-keydown)
  (.addEventListener js/window "keydown" screen-stack/on-keydown)
  (rd/render @root [render-screen]))
