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
   ".me" {:color "deeppink"}
   ".is-host" {:position "absolute"
               :left "50%"
               :top "1rem"
               :transform "translateX(-50%)"
               :color "#999"}
   "&:hover.empty" {:outline "1px solid #666"}})

(defstyles slots-container-class []
  {:display "flex"})

(defn- handle-change-slot [slot room player]
  (when (and (not= (:slot player) slot)
             (not-any? #(= (:slot %) slot) (:players room)))
    (room-manager/change-slot slot)))

(defn player-slot [slot room player local-user]
  (let [empty-slot? (nil? player)
        me? (= (:id player) (:id local-user))
        host? (= (:id player) (:host room))
        readied? (= (:state player) :readied)]
    [:div {:key slot
           :class [(when empty-slot? "empty")
                   (player-slot-class empty-slot? readied?)]
           :title (when empty-slot? "切换到此位置")
           :on-click #(handle-change-slot slot room player)}
     (when host?
       [:div {:class "is-host"} "房主"])
     [:div {:class (when me? "me")} (:username player)]]))

(defn player-slots [room players local-user]
  [:div {:class (slots-container-class)}
   (map-indexed (fn [slot player]
                  (player-slot slot room player local-user)) players)])

