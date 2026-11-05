(ns tetris.screens.multiplayer.start-button
  (:require ["@base-ui/react/button" :refer [Button]]
            [tetris.i18n.core :as i18n]
            [tetris.screens.multiplayer.room-manager :as room-manager]
            [tetris.screens.screen-stack :as screen-stack]
            [tetris.screens.styles.button :as styles.button]))

(defn- ^:async handle-start [button-command]
  (case button-command
    :ready (room-manager/toggle-ready)
    :unready (room-manager/toggle-ready)
    :start-game (let [resp (await (room-manager/enter-game))]
                  (when (= (:code resp) :ok)
                    (screen-stack/replace-screen! :multiplayer)))))

(defn start-button [local-user room players]
  (let [me (first (filter #(= (:id %) (:id local-user)) players))
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
