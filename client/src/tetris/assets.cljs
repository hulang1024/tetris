(ns tetris.assets 
  (:require ["pixi.js" :as pixi]))

(defn ^:async load-piece-styles [styles]
  (doseq [piece-style (if (string? styles) [styles] styles)
          :let [alias (str "gameplay" "/" piece-style)
                src (str "assets/" alias ".png")]]
    (await (pixi/Assets.load #js {:alias alias :src src}))))

(defn get-piece-styles []
  ["b1" "b2" "b3" "b4" "b5" "b8" "b9" "b10" "b11" "b12" "b13"])
