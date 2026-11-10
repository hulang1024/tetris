(ns tetris.scenes.render.gameplay.hud 
  (:require
    ["pixi.js" :as pixi]
    [tetris.scenes.render.gameplay.piece :refer [piece-container render-piece]]))

(def hud-layout {:text-font-size 24
                 :title-font-size 24
                 :value-font-size 32})

(defn stat-item-y [item-n t]
  (let [tfs (:title-font-size hud-layout)
        vfs (:value-font-size hud-layout)
        tv-gap 8
        item-gap 24]
    (+ (* item-n (+ item-gap tfs tv-gap vfs))
       (if (= t :t) 0 (+ tv-gap tfs)))))

(defn hold [layout]
  (let [piece (piece-container
                {:label "hold"
                 :x (:x layout)
                 :y (:y layout)
                 :cell-size (get-in layout [:cell :size])})]
    piece))

(defn preview [layout preview-count]
  (let [container (pixi/Container. #js {:label "next"
                                        :x (:x layout)
                                        :y (:y layout)})]
    (dotimes [n preview-count]
      (.addChild container
                 (piece-container {:label (str "preview-" n)
                                   :cell-size (get-in layout [:cell :size])})))
    container))

(def stats-text-style
  {:fontFamily "zpix"
   :dropShadow
   #js {:color "#000000"
        :blur 4
        :distance 8}
   :fill "#eeeeee"})

(defn text-view [y anchor label text & [style]]
  (pixi/Text.
    #js {:label label
         :y y
         :anchor (clj->js anchor)
         :text text
         :style (clj->js
                  (merge stats-text-style
                         {:fontWeight "bold"
                          :fontSize (:text-font-size hud-layout)}
                         style))}))

(defn title-view [y anchor text]
  (text-view y anchor "title" text
             {:fontSize (:title-font-size hud-layout)}))

(defn number-view [y anchor label text]
  (pixi/Text.
    #js {:label label
         :y y
         :anchor (clj->js anchor)
         :text text
         :style (clj->js
                  (merge stats-text-style
                         {:fontWeight "bolder"
                          :fontSize (:value-font-size hud-layout)}))}))

(defn left-stats-view [layout]
  (let [container (pixi/Container.
                    #js {:label "left"
                         :anchor #js {:x 0 :y 0}})
        anchor {:x 1 :y 0}]
    (.addChild container
               (title-view (stat-item-y 0 :t) anchor "SPEED LV")
               (number-view (stat-item-y 0 :v) anchor "speed-level" "0")
               (title-view (stat-item-y 1 :t) anchor "LINES")
               (number-view (stat-item-y 1 :v) anchor "lines" "0"))
    (set! (.-y container) (- (get-in layout [:matrix :height])
                             (.-height container)))
    (set! (.-x container) (- (.-width container)
                             (- (get-in layout [:matrix :margin-x]) 32)))
    container))

(defn right-stats-view [layout player-name]
  (let [container (pixi/Container.
                    #js {:label "right"
                         :anchor #js {:x 0 :y 0}})
        anchor {:x 0 :y 0}]
    (doto container
      (.addChild (title-view (stat-item-y 0 :t) anchor player-name))
      (.addChild (text-view (stat-item-y 0 :v) anchor "status" ""
                            {:fill "#ff1111"}))
      (.addChild (title-view (stat-item-y 1 :t) anchor "SCORE"))
      (.addChild (number-view (stat-item-y 1 :v) anchor "score" "0")))
    (set! (.-y container) (- (get-in layout [:matrix :height])
                             (.-height container)))
    (set! (.-x container) (get-in layout [:next :x]))
    container))

(defn create [layout {:keys [preview-count player-name]}]
  (let [hud (pixi/Container. #js {:label "hud"})]
    (.addChild hud
               (hold (:hold layout))
               (preview (:next layout) preview-count)
               (left-stats-view layout)
               (right-stats-view layout player-name))
    hud))

(defn- render-hold [view data]
  (render-piece @view
                (.getChildByLabel ^js (:hud @view) "hold")
                (:hold data)))

(defn- render-next [view data]
  (doseq [[piece-v piece-d]
          (map vector
               (.-children (.getChildByLabel ^js (:hud @view) "next"))
               (:next data))]
    (render-piece @view piece-v piece-d)))

(defn render! [view data game-state]
  (render-hold view data)
  (render-next view data)

  (let [left (.getChildByLabel ^js (:hud @view) "left")
        right (.getChildByLabel ^js (:hud @view) "right")
        speed-level-view (.getChildByLabel left "speed-level")
        lines-view (.getChildByLabel left "lines")
        score-view (.getChildByLabel right "score")
        status-view (.getChildByLabel right "status")]
    (set! (.-text speed-level-view) (:speed-level game-state))
    (set! (.-text lines-view) (:lines-cleared game-state))
    (set! (.-text score-view) (:score game-state))
    (cond
      (= (:status game-state) :quitted) (set! (.-text status-view) "QUITTED")
      (= (:status game-state) :left-room) (set! (.-text status-view) "LEFT ROOM")
      :else nil)))

