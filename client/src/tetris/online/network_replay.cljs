(ns tetris.online.network-replay 
  (:require
    [tetris.core.input :as input]))

(defn make-input-buffer [uid] {:uid uid :input-frames []})

(defn append-input-frame [buffer [frame-no pressed-buttons]]
  (let [pressed-actions (filterv #(contains? input/actions %) pressed-buttons)]
    (update buffer :input-frames conj [frame-no pressed-actions])))

(defn append-input-frames [buffer input-frames]
  (reduce (fn [buffer input-frame] (append-input-frame buffer input-frame))
          buffer
          input-frames))

(defn make-replayer [uid]
  (assoc (make-input-buffer uid) :last-input input/empty-state))

(defn step [replayer]
  (if-let [input-frame (first (:input-frames replayer))]
    (let [[_ pressed-buttons] input-frame
          input-state (input/handle (:last-input replayer) pressed-buttons)]
      [input-state
       (-> replayer
           (update :input-frames subvec 1)
           (assoc :last-input input-state))])
    [nil replayer]))
