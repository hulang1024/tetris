(ns tetris.screens.lobby 
  (:require
    [reagent.core :as r]
    [tetris.screens.menu :as menu]
    [tetris.screens.menu-overlay :refer [menu-overlay]]
    [tetris.screens.screen :as screen]
    [tetris.screens.screen-stack :as screen-stack]
    [tetris.screens.tip-buttons-overlay :as tip-buttons-overlay]))

(defmethod screen/init :lobby [_]
  {:menu-state
   (r/atom
     (menu/make
       [(menu/item :join-room nil 1)
        (menu/item :create-room nil 2 {:screen :create-room})
        (menu/item :list-rooms nil 3 {:screen :list-rooms})]
       :join-room))})

(defmethod screen/render :lobby [this]
  [:div
   [menu-overlay
    {:state (:menu-state @this)
     :on-enter (fn [item]
                 (when-let [data (:data item)]
                   (screen-stack/push-screen! (:screen data) (dissoc data :screen))))}]
   (tip-buttons-overlay/render)])
