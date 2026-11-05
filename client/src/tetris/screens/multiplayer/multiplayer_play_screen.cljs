(ns tetris.screens.multiplayer.multiplayer-play-screen
  (:require
    [tetris.screens.screen :as screen]))

(defmethod screen/build :multiplayer [_this]
  [:div "ok"])
