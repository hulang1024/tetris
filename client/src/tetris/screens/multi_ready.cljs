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
    [tetris.screens.styles.button :as styles.button]))

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

(defn- ^:async leave-room []
  (screen-stack/pop-screen!))

(defn- get-player-info [player user-infos]
  (merge player (first (filter #(= (:id %) (:id player)) user-infos))))

(defn- get-me [players]
  (first (filter #(= (:id %) (get-in @api/state [:user :id])) players)))

(defn- ^:async handle-start [button-command]
  (case button-command
    :ready-game (api/post "/room/toggle-ready")
    :cancel-ready-game (api/post "/room/toggle-ready")
    :start-game (let [resp (await (api/post "/room/enter-game"))]
                  (when (= (:code resp) :ok)
                    (screen-stack/replace-screen! :multiplayer)))))

(defmethod screen/init :multi-ready [_ {:keys [room users]}]
  (let [state (r/atom {:room (dissoc room :players)
                       :players
                       (mapv (fn [slot]
                               (let [player (first (filter #(= (:slot %) slot) (:players room)))]
                                 (get-player-info player users)))
                             (range (:players-max room)))})]
    (api/subscribe-event
      :user-joined
      (fn [{{:keys [room user]} :data}]
        (let [player (first (filter #(= (:id %) (:id user)) (:players room)))]
          (swap! state assoc-in [:players (:slot player)]
                 (get-player-info player [user])))))
    (api/subscribe-event
      :user-left
      (fn [{{:keys [room uid]} :data}]
        (let [player (first (filter #(= (:id %) uid) (:players room)))]
          (swap! state (fn [state]
                         (-> (assoc-in state [:players (:slot player)] nil)
                             (assoc-in [:room :host] (:host room))))))))
    (api/subscribe-event
      :ready-toggled
      (fn [{{:keys [room uid]} :data}]
        (let [player (first (filter #(= (:id %) uid) (:players room)))]
          (swap! state assoc-in [:players (:slot player) :state] (:state player)))))
    state))

(defmethod screen/on-exiting :multi-ready []
  (doseq [event-type [:user-joined :user-left :ready-toggled]]
    (api/unsubscribe-event event-type))
  (api/post "/room/leave-room"))

(defmethod screen/render :multi-ready [this]
  [:div
   [:div {:class (slots-container-class)}
    (map #(player-slot % (get-in @this [:room :host])) (:players @this))]

   [:div {:style {:margin-top "2rem"}}
    [:> Button {:class (styles.button/overlay-button {:bg-color :danger :full? false})
                :on-click #(leave-room)}
     (i18n/text :leave-game)]
    (let [me (get-me (:players @this))
          readied? (= (:state me) "readied")
          joined-count (count (filter some? (:players @this)))
          readied-count (count (filter #(= (:state %) "readied") (:players @this)))
          players-min (get-in @this [:room :players-min])
          host? (= (:id me) (get-in @this [:room :host]))
          button-command (if readied?
                           (if host?
                             (cond
                               (< readied-count players-min) :cancel-ready-game
                               (>= readied-count joined-count) :start-game
                               (< readied-count joined-count) :force-start-game)
                             :cancel-ready-game)
                           :ready-game)]
      [:> Button {:class (styles.button/overlay-button
                           {:bg-color (if (= button-command :start-game)
                                        :success
                                        :primary)
                            :full? false})
                  :style {:margin-left "1rem"}
                  :on-click #(handle-start button-command)}
       (i18n/text button-command)])]])
