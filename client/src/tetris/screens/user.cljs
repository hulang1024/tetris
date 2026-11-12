(ns tetris.screens.user
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [tetris.i18n.core :as i18n]))

(defstyles display-user-class []
  {:width "6rem"
   :height "100%"
   :display "flex"
   :text-align "center"
   :justify-content "center"
   :align-items "center"
   :background "rgba(0,0,0,0.1)"
   "&:hover" {:background "rgba(0,0,0,0.2)"}})

(defn display-user [api-state local-user on-click]
  [:div {:class (display-user-class)}
   (case api-state
     :offline
     [:span {:style {:width "100%"
                     :justify-self "center"
                     :align-self "center"
                     :font-size "1rem"}
             :on-click on-click}
      (i18n/text :click-login)]

     :online
     [:div (:username local-user)]

     nil)])

