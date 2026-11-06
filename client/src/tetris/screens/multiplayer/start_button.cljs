(ns tetris.screens.multiplayer.start-button
  (:require [tetris.i18n.core :as i18n]
            [tetris.screens.multiplayer.multiplayer-play-screen :as multiplayer-play-screen]
            [tetris.screens.multiplayer.room-manager :as room-manager]
            [tetris.screens.screen-navigator :as screen-navigator]
            [tetris.ui.button :refer [button]]))

(defn- ^:async handle-start [button-command]
  (case button-command
    :ready (room-manager/toggle-ready)
    :unready (room-manager/toggle-ready)
    :start-game (let [resp (await (room-manager/enter-game))]
                  (when (= (:code resp) :ok)
                    (screen-navigator/replace-screen! (multiplayer-play-screen/make))))))

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
             :on-click #(handle-start button-command)}
            (i18n/text [:multiplayer button-command]))))
