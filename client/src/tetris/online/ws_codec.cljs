(ns tetris.online.ws-codec 
  (:require
    [clojure.set :as set]))

(def ^:private button-id
  {:soft-drop 1 :move-left  2 :move-right 3
   :rotate-cw 4 :rotate-ccw 5 :rotate-180 6
   :hard-drop 7 :hold 8})

(def ^:private id-button (set/map-invert button-id))

(def ^:private event-types {1 :player-inputs})

(defn encode-input-frames [uid input-frames]
  (let [buf (js/Uint8Array. (+ 5 (* 13 (count input-frames))))
        dv  (js/DataView. (.-buffer buf))]
    (.setUint8 dv 0 1)
    (.setUint32 dv 1 uid)
    (doseq [[i [frame-no pressed-buttons]] (map-indexed vector input-frames)
            :let [off (+ 5 (* i 13))]]
      (.setUint32 dv off frame-no)
      (.setUint8  dv (+ off 4) (count pressed-buttons))
      (doseq [[j button] (map-indexed vector pressed-buttons)]
        (.setUint8 dv (+ off 5 j) (button-id button 0))))
    buf))

(defn decode-input-frames [buf]
  (let [dv  (js/DataView. (.-buffer buf))
        n (quot (- (.-length buf) 5) 13)]
    {:type (event-types (.getUint8 dv 0) :unknown)
     :data {:uid (.getUint32 dv 1)
            :input-frames
            (mapv (fn [i]
                    (let [off (+ 5 (* i 13))
                          frame-no (.getUint32 dv off)
                          button-cnt (.getUint8 dv (+ off 4))]
                      [frame-no
                       (mapv (fn [j] (id-button (.getUint8 dv (+ off 5 j))))
                             (range button-cnt))]))
                  (range n))}}))

(defn ->server-event [raw-data]
  (if (string? raw-data)
    (-> (js->clj (js/JSON.parse raw-data)
                 :keywordize-keys true)
        (update :type keyword))
    (decode-input-frames (js/Uint8Array. raw-data))))
