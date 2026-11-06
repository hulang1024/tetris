(ns tetris.main
  (:require
    [cljss.core :as css]
    [tetris.audio :as audio]
    [tetris.online.api :as api]
    [tetris.ui.styles.global :as styles]
    [tetris.screens.main-menu-screen :as main-menu-screen]
    [tetris.screens.screen-container :as screen-container]
    [tetris.screens.screen-navigator :as screen-navigator]))

(defn ^:async init []
  (when ^boolean goog/DEBUG
    (add-tap println))
  (styles/load-global)
  (await (audio/load-sounds :ui))
  (screen-container/load)
  (screen-navigator/push-screen! (main-menu-screen/make))

  (api/load-session-user!))

(defn ^:async ^:dev/after-load reload []
  (tap> "reload")
  (css/remove-styles!)
  (styles/load-global)
  (screen-container/load))
