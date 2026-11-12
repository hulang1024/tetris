(ns tetris.screens.multiplayer.player-slot
  (:require-macros [cljss.core])
  (:require [cljss.core :as css :refer-macros [defstyles]]
            [tetris.online.room-api :as room-api]))

(defstyles slots-container-class []
  {:display "flex"})

(defstyles player-slot-class [empty-slot? player-state]
  {:position "relative"
   :margin-right "1rem"
   :width "6rem"
   :height "10rem"
   :padding "2rem"
   :display "flex"
   :flex-direction "column"
   :justify-content "center"
   :background (case player-state
                 :readied "rgba(0,255,0,0.2)"
                 :playing "#ccc"
                 (if empty-slot? "#f4f4f4" "#eee"))
   :outline (when-not empty-slot? "2px solid #333")
   :text-align "center"
   ".me" {:color "deeppink"}
   ".is-host" {:position "absolute"
               :left "50%"
               :top "1rem"
               :transform "translateX(-50%)"
               :color "#999"}
   "&:hover.empty" {:outline "1px solid #666"}})

(defstyles role-class []
  {:position "absolute"
   :right "1rem"
   :top "1rem"
   :color "blueviolet"})

(defstyles player-status-class []
  {:margin-top "1rem"
   :font-size "1rem"})

(defn- handle-change-slot [slot room player]
  (when (and (not= (:slot player) slot)
             (not-any? #(= (:slot %) slot) (:players room)))
    (room-api/change-slot slot)))

(defn player-slot [slot room player local-user]
  (let [empty-slot? (nil? player)
        me? (= (:id player) (:id local-user))
        host? (= (:id player) (:host room))]
    [:div {:key slot
           :class [(when empty-slot? "empty")
                   (player-slot-class empty-slot? (:state player))]
           :title (when empty-slot? "切换到此位置")
           :on-click #(handle-change-slot slot room player)}
     (when host?
       [:div {:class "is-host"} "房主"])
     (when-not empty-slot?
       [:div {:class (role-class)} (if (= (:role player) :spectator) "观众" "")])
     [:div {:class (when me? "me")} (:username player)]
     [:div {:class (player-status-class)
            :style {:display (if (= (:state player) :inprogress) "block" "none")}}
      "游戏中"]]))

(defn player-slots [room players local-user]
  [:div {:class (slots-container-class)}
   (map-indexed (fn [slot player]
                  (player-slot slot room player local-user)) players)])

