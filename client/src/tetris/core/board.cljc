(ns tetris.core.board 
  (:require [tetris.core.piece :refer [Piece PieceKind]]
            [tetris.core.rs :as rs]))

(def Cell [:maybe [:map [:id :int] [:kind PieceKind]]])

(def Board [:vector [:vector Cell]])

(def ^:const skyline-rows 2)
(def ^:const board-rows (+ 20 skyline-rows))
(def ^:const board-cols 10)

(def empty-line (vec (repeat board-cols nil)))
(def empty-board (vec (repeat board-rows empty-line)))

(defn filled? [board r c]
  (get-in board [r c]))

(defn valid-position? [row col]
  (and (<= 0 row (dec board-rows))
       (<= 0 col (dec board-cols))))

(defn collide?
  {:malli/schema [:=> [:cat Board Piece :int :int] :boolean]}
  [board piece row col]
  (boolean
    (some (fn [[cr cc]]
            (let [r (+ row cr)
                  c (+ col cc)]
              (or (not (valid-position? r c))
                  (filled? board r c))))
          (rs/cell-indices piece))))

(defn lock-piece
  {:malli/schema [:=> [:cat Board Piece :int :int] Board]}
  [board piece row col]
  (reduce (fn [board [i [r c]]]
            (assoc-in board [(+ row r) (+ col c)] {:id (+ (:id piece) i) :kind (:kind piece)}))
          board
          (map-indexed vector (rs/cell-indices piece))))

(defn find-full-row-indices
  {:malli/schema [:=> [:cat Board] [:set :int]]}
  [board]
  (->> board (keep-indexed (fn [r xs] (when (every? some? xs) r))) set))

(defn find-cells-to-clear
  {:malli/schema [:=> [:cat Board [:set :int]] [:vector Cell]]}
  [board full-row-indices]
  (->> board
       (keep-indexed (fn [r xs] (when (contains? full-row-indices r) xs)))
       (mapcat identity)
       vec))

(defn clear-rows
  {:malli/schema [:=> [:cat Board [:set :int]] Board]}
  [board row-indices]
  (->> board
       (keep-indexed (fn [r xs] (when-not (contains? row-indices r) xs)))
       (into (vec (repeat (count row-indices) empty-line)))))

(defn lock-out?
  {:malli/schema [:=> [:cat :int] :boolean]}
  [row] (<= row 0))
