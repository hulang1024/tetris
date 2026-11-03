(ns tetris.screens.multiplayer 
  (:require
    [tetris.screens.screen :as screen]))

(defmethod screen/init :multiplayer [_]
  {})

(defmethod screen/render :multiplayer [_this]
  [:div "ok"])
