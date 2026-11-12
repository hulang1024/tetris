(ns tetris.core.ruleset.classic
  (:require
    [tetris.core.ruleset :refer [fall-speed line-clear-delay lock-delay
                                 soft-drop-speed]]
    [tetris.core.ruleset.pgen-hr :as pgen-hr]
    [tetris.core.ruleset.scoring-nes :as scoring-nes]
    [tetris.core.ruleset.speedlv-nes :as speedlv-nes]
    [tetris.core.tick :as tick]))

(defn make-classic-ruleset
  {:malli/schema [:=> [:cat some? :int] tick/GameOptions]}
  [seed speed-level]
  {:ruleset :classic
   :rotation-system :nrs-nes
   :piece-generator (pgen-hr/make seed 1 2)
   :scoring (scoring-nes/make)
   :speed-level-system (speedlv-nes/make 0)
   :preview-count 1
   :ghost-enabled? false
   :initial-hold-enabled? false
   :initial-rotation-enabled? false
   :hold-allowed? false
   :hard-drop-allowed? false
   :rotate-180-allowed? false
   :das-cancel-on-direction-change? false
   :das-cancel-on-lock? false
   :lock-reset-max-times nil
   :speed-level (min (max speed-level 0) 18)
   :das 16
   :arr 6
   :dcd 16
   :sdf 1})

(def ^:private level-fall-interval-table
  [48 43 38 33 28 23 18 13 8 6
   5 5 5 4 4 4 3 3 3 2
   2 2 2 2 2 2 2 2 2 1])

(defmethod fall-speed :classic [state]
  (/ 1 (get level-fall-interval-table (:speed-level state) 1)))

(defmethod soft-drop-speed :classic [state]
  (* 0.5 (get-in state [:options :sdf])))

(defmethod line-clear-delay :classic [_] 4)

(defmethod lock-delay :classic [_] 10)
