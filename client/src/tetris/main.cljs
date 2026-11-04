(ns tetris.main
  (:require
    [cljss.core :as css]
    [tetris.api :as api]
    [tetris.audio :as audio]
    [tetris.screens.controls-options]
    [tetris.screens.gameplay-options]
    [tetris.screens.main-menu]
    [tetris.screens.replay]
    [tetris.screens.screen-stack :as screen-stack]
    [tetris.screens.solo]
    [tetris.screens.multi-ready]
    [tetris.screens.multiplayer]
    [tetris.screens.styles :as styles]))

(defn ^:async init []
  (when ^boolean goog/DEBUG
    (add-tap println))
  (styles/load-global)
  (await (audio/load-sounds :ui))
  (screen-stack/render)
  (screen-stack/push-screen! :main-menu)

  (api/load-user!))

(defn ^:async ^:dev/after-load reload []
  (tap> "reload")
  (css/remove-styles!)
  (styles/load-global)
  (screen-stack/render))
