(ns tetris.screens.multiplayer.player-slot
  (:require-macros [cljss.core])
  (:require [cljss.core :as css :refer-macros [defstyles]]
            [tetris.screens.multiplayer.room-manager :as room-manager]))

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
   :outline (when-not empty-slot? "2px solid #333")
   :text-align "center"
   "&:hover.empty" {:outline "1px solid #666"}})

(defstyles slots-container-class []
  {:display "flex"})

(defn- handle-change-slot [slot room player]
  (when (and (not= (:slot player) slot)
             (not-any? #(= (:slot %) slot) (:players room)))
    (room-manager/change-slot slot)))

(defn player-slot [slot room player]
  (let [empty-slot? (nil? player)
        host? (= (:id player) (:host room))
        readied? (= (:state player) :readied)]
    [:div {:key slot
           :class [(when empty-slot? "empty")
                   (player-slot-class empty-slot? readied?)]
           :title (when empty-slot? "切换到此位置")
           :on-click #(handle-change-slot slot room player)}
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

