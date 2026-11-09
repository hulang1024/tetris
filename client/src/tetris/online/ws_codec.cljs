(ns tetris.online.ws-codec)

(def ^:private button-bit
  {:soft-drop 0 :move-left 2 :move-right 4
   :rotate-cw 8 :rotate-ccw 16 :rotate-180 32
   :hard-drop 64 :hold 128})

(def event-types
  {6 :player-inputs})

(def ^:private button-order [:soft-drop :move-left :move-right 
                             :rotate-cw :rotate-ccw :rotate-180 
                             :hard-drop :hold])

(defn encode-input-frames [uid input-frames]
  (let [buf (js/Uint8Array. (+ 6 (* 4 (count input-frames))))
        dv  (js/DataView. (.-buffer buf))]
    (.setUint8 dv 0 6)
    (.setUint32 dv 2 uid)
    (doseq [[i [frame-no pressed-buttons]] (map-indexed vector input-frames)
            :let [off (+ 6 (* i 4))]]
      (.setUint8 dv off        (bit-and (bit-shift-right frame-no 16) 0xFF))
      (.setUint16 dv (+ off 1) (bit-and frame-no 0xFFFF))
      (.setUint8 dv (+ off 3)
                 (reduce (fn [v b] (bit-or v (button-bit b 0))) 0 pressed-buttons)))
    buf))

(defn decode-input-frames [buf]
  (let [dv  (js/DataView. (.-buffer buf))
        n (quot (- (.-length buf) 6) 4)]
    {:type (event-types (.getUint8 dv 0) :unknown)
     :data {:uid (.getUint32 dv 2)
            :input-frames
            (mapv (fn [i]
                    (let [w (.getUint32 dv (+ 6 (* i 4)))
                          pressed (bit-and w 0xFF)]
                      [(bit-shift-right w 8)
                       (vec (keep-indexed
                              (fn [i button] (when (bit-test pressed i) button))
                              button-order))]))
                  (range n))}}))

(defn ->server-event [raw-data]
  (if (string? raw-data)
    (-> (js->clj (js/JSON.parse raw-data)
                 :keywordize-keys true)
        (update :type keyword))
    (decode-input-frames (js/Uint8Array. raw-data))))
