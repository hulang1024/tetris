(ns tetris.screens.multiplayer.player-slot
  (:require-macros [cljss.core])
  (:require [cljss.core :as css :refer-macros [defstyles]]))

(defstyles player-slot-class [empty-slot? readied?]
  {:position "relative"
   :margin-right "1rem"
   :width "6rem"
   :height "10rem"
   :padding "2rem"
   :display "flex"
   :flex-direction "column"
   :justify-content "center"
   :background (if readied? "rgba(0,255,0,0.2)" (if empty-slot? "#f4f4f4" "#eee"))
   :outline (when-not empty-slot? "1px solid black")
   :text-align "center"})

(defstyles slots-container-class []
  {:display "flex"})

(defn player-slot [slot room player]
  (let [empty-slot? (nil? player)
        host? (= (:id player) (:host room))
        readied? (= (:state player) :readied)]
    [:div {:key slot
           :class (player-slot-class empty-slot? readied?)}
     (when host?
       [:div {:style {:position "absolute"
                      :right "0.8rem"
                      :top "0.8rem"
                      :font-size "0.8rem"}}
        "[房主]"])
     [:div (:username player)]
     (when-not empty-slot?
       [:div {:style {:margin-top "2rem"}}
        (if readied? "已准备" "未准备")])]))

(defn player-slots [room players]
  [:div {:class (slots-container-class)}
   (map-indexed (fn [slot player] (player-slot slot room player)) players)])

