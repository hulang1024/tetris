(ns tetris.scenes.game 
  (:require
    [tetris.core.ruleset.classic :refer [make-classic-ruleset]]
    [tetris.core.ruleset.modern :refer [make-modern-ruleset]]))

(defn random-seed [] (int (* (rand) 100000)))

(defn make-game-options [{:keys [seed speed-level rs handling] :as game-config}]
  (merge-with #(or %2 %1)
              (case (:mode game-config)
                :modern (make-modern-ruleset seed speed-level)
                :classic (make-classic-ruleset seed speed-level))
              (select-keys game-config [:preview-count :piece-style])
              {:rotation-system rs}  
              handling))

(defn mode->game-config [mode]
  (case mode
    :marathon {:mode :modern :speed-level 1}
    :classic {:mode :classic :speed-level 0}))
