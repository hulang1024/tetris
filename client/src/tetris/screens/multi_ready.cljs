(ns tetris.screens.multi-ready 
  (:require-macros [cljss.core])
  (:require
    ["@base-ui/react/button" :refer [Button]]
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.api :as api]
    [tetris.i18n.core :as i18n]
    [tetris.screens.screen :as screen]
    [tetris.screens.screen-stack :as screen-stack]
    [tetris.screens.styles.button :as styles.button]
    [tetris.audio :as audio]))

(defstyles player-slot-class [empty-slot? readied?]
  {:position "relative"
   :margin-right "1rem"
   :width "6rem"
   :height "10rem"
   :padding "2rem"
   :display "flex"
   :flex-direction "column"
   :justify-content "center"
   :background (if readied? "rgba(0,255,0,0.2)" "#eee")
   :outline (when-not empty-slot? "1px solid black")
   :text-align "center"})

(defstyles slots-container-class []
  {:display "flex"})

(defn- get-player-info [player user-infos]
  (merge player (first (filter #(= (:id %) (:id player)) user-infos))))

(defn- ^:async handle-start [button-command]
  (case button-command
    :ready (api/post "/room/toggle-ready")
    :unready (api/post "/room/toggle-ready")
    :start-game (let [resp (await (api/post "/room/enter-game"))]
                  (when (= (:code resp) :ok)
                    (screen-stack/replace-screen! :multiplayer)))))

(defn- player-slot [player host]
  (let [empty-slot? (nil? player)
        host? (= (:id player) host)
        readied? (= (:state player) "readied")]
    [:div {:class (player-slot-class empty-slot? readied?)}
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

(defn- start-button [user players room]
  (let [me (first (filter #(= (:id %) (:id user)) players))
        host? (= (:id me) (:host room))
        readied? (= (:state me) "readied")
        joined-count (count (filter some? players))
        readied-count (count (filter #(= (:state %) "readied") players))
        players-min (:players-min room)
        button-command (if readied?
                         (if host?
                           (cond
                             (< readied-count players-min) :unready
                             (>= readied-count joined-count) :start-game
                             (< readied-count joined-count) :force-start-game)
                           :unready)
                         :ready)]
    [:> Button {:class (styles.button/overlay-button
                         {:bg-color (if (= button-command :start-game)
                                      :success
                                      :primary)
                          :full? false})
                :style {:margin-left "1rem"}
                :on-click #(handle-start button-command)}
     (i18n/text [:multiplayer button-command])]))

(defmethod screen/init :multi-ready [_ {:keys [room users]}]
  (audio/load-sounds :multiplayer)
  (let [state (r/atom {:room (dissoc room :players)
                       :players
                       (mapv (fn [slot]
                               (let [player (first (filter #(= (:slot %) slot) (:players room)))]
                                 (get-player-info player users)))
                             (range (:players-max room)))})]
    (api/subscribe-event
      :player-joined
      (fn [{{:keys [player user]} :data}]
        (swap! state assoc-in [:players (:slot player)] (get-player-info player [user]))
        (audio/play :multiplayer/player-joined)))
    (api/subscribe-event
      :player-left
      (fn [{{:keys [room player]} :data}]
        (swap! state (fn [state]
                       (-> (assoc-in state [:players (:slot player)] nil)
                           (assoc-in [:room :host] (:host room)))))
        (audio/play :multiplayer/player-left)))
    (api/subscribe-event
      :player-ready-toggled
      (fn [{{:keys [room player]} :data}]
        (let [ready? (= (:state player) "readied")
              me? (= (:id player) (get-in @api/state [:user :id]))
              joined-count (count (filter some? (:players room)))
              readied-count (count (filter #(= (:state %) "readied") (:players room)))]
          (swap! state assoc-in [:players (:slot player) :state] (:state player))
          (if (= joined-count readied-count)
            (audio/play :multiplayer/player-ready-all)
            (when me?
              (audio/play (if ready?
                            :multiplayer/player-ready
                            :multiplayer/player-unready)))))))
    state))

(defmethod screen/on-exiting :multi-ready []
  (doseq [event-type [:player-joined :player-left :player-ready-toggled]]
    (api/unsubscribe-event event-type))
  (api/post "/room/leave-room"))

(defmethod screen/render :multi-ready [this]
  [:div
   [:div {:class (slots-container-class)}
    (map #(player-slot % (get-in @this [:room :host])) (:players @this))]

   [:div {:style {:margin-top "2rem"}}
    [:> Button {:class (styles.button/overlay-button {:bg-color :danger :full? false})
                :on-click #(screen-stack/pop-screen!)}
     (i18n/text [:multiplayer :leave])]
    (start-button (:user @api/state) (:players @this) (:room @this))]])
