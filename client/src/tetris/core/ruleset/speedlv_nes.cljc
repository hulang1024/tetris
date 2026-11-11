(ns tetris.core.ruleset.speedlv-nes 
  (:require
    [tetris.core.speedlv :refer [update-level]]))

(defn make [initial-level] {:type :nes :initial-level initial-level})

(defmethod update-level :nes [state game-state]
  (let [curr-level (:speed-level game-state)
        curr-lines (+ (* 10 (:initial-level state)) (:lines-cleared game-state))
        next-level-lines (* (inc curr-level) 10)]
    [(if (>= curr-lines next-level-lines)
       (inc curr-level)
       curr-level)
     state]))

