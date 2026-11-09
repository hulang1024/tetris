(ns tetris.screens.multiplayer.start-button
  (:require [tetris.i18n.core :as i18n]
            [tetris.screens.multiplayer.room-manager :as room-manager]
            [tetris.ui.button :refer [button]]))

(defn- ^:async start-game [force?]
  (let [resp (await (room-manager/enter-game force?))]
    (when (not= (:code resp) :ok)
      (js/alert "开始失败"))))

(defn- handle-start [button-command]
  (case button-command
    :ready (room-manager/toggle-ready)
    :unready (room-manager/toggle-ready)
    :start-game (start-game false)
    :force-start-game (start-game true)))

(defn start-button [local-user room players]
  (let [me (first (filter #(= (:id %) (:id local-user)) players))
        host? (= (:id me) (:host room))
        readied? (= (:state me) :readied)
        players-min (:players-min room)
        {:keys [joined-count readied-count]} (room-manager/room-stats room)
        button-command (if readied?
                         (if host?
                           (cond
                             (< readied-count players-min) :unready
                             (>= readied-count joined-count) :start-game
                             (< readied-count joined-count) :force-start-game)
                           :unready)
                         :ready)]
    (button {:bg-color (if (= button-command :start-game) :success :primary)
             :full? false
             :style {:margin-left "1rem"}
             :disabled (= (:state room) :inprogress)
             :on-click #(handle-start button-command)}
            (i18n/text [:multiplayer button-command]))))
