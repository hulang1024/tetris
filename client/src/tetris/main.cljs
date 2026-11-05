(ns tetris.main
  (:require
    [cljss.core :as css]
    [tetris.online.api :as api]
    [tetris.audio :as audio]
    [tetris.screens.screen-stack :as screen-stack]
    [tetris.screens.controls-options-screen]
    [tetris.screens.gameplay-options-screen]
    [tetris.screens.main-menu-screen]
    [tetris.screens.replay-screen]
    [tetris.screens.solo-play-screen]
    [tetris.screens.multiplayer.multiplayer-ready-screen]
    [tetris.screens.multiplayer.multiplayer-play-screen]
    [tetris.screens.styles.global :as styles]))

(defn ^:async init []
  (when ^boolean goog/DEBUG
    (add-tap println))
  (styles/load-global)
  (await (audio/load-sounds :ui))
  (screen-stack/render)
  (screen-stack/push-screen! :main-menu)

  (api/load-session-user!))

(defn ^:async ^:dev/after-load reload []
  (tap> "reload")
  (css/remove-styles!)
  (styles/load-global)
  (screen-stack/render))
