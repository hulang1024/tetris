(ns tetris.audio
  (:require ["howler" :as howler]))

(def ^:private cached-sounds (atom {}))

(defn sound [id] (id @cached-sounds))

(defn play [id]
  (when-let [s (sound (keyword id))]
    (.play s)))

(defn- log [s]
  (tap> (str "aduio - " s)))

(defn filename->id [filename]
  (keyword (.substr filename 0 (- (.-length filename) 4))))

(defn- effect-list []
  (for [name (into ["rotate" "land" "hard-drop" "lock" "hold" "fail"]
                   (map #(str "clear-" %) (range 1 7)))
        :let [id (keyword (str "effect/" name))
              src (str "assets/gameplay/effect/chiptune/" name ".ogg")]]
    [id src]))

(defn- sample-list [sample-type]
  (for [name (mapv #(str %) (range 1 29))
        :let [id (keyword (str "sample/" sample-type "-" name))
              src (str "assets/gameplay/sample/" sample-type "/" name ".ogg")]]
    [id src]))

(defn- ui-list []
  (for [filename ["menu-hover.wav"
                  "menu-select.wav"
                  "menu-back.wav"
                  "menu-gameplay-select.wav"
                  "screen-back.mp3"
                  "button-click.mp3"]
        :let [id (filename->id filename)
              src (str "assets/ui/" filename)]]
    [id src]))

(defn- multiplayer-list []
  (for [name ["player-joined"
              "player-left"
              "player-ready"
              "player-unready"
              "player-ready-all"]
        :let [id (keyword (str "multiplayer/" name))
              src (str "assets/multiplayer/" name ".wav")]]
    [id src]))

(defn load-sounds [sound-set-key]
  (tap> "load sounds")
  (doseq [[id src] (filter #(not ((first %) @cached-sounds))
                           (get {:ui (ui-list)
                                 :gameplay (concat (effect-list) (sample-list "bass"))
                                 :multiplayer (multiplayer-list)}
                                sound-set-key))
          :let [sound (howler/Howl. (clj->js {:src [src]
                                              :volume 0.1}))]]
    (.once sound "load" (fn [_] (log (str "loaded: " id " -> " src))))
    (.on sound "play" (fn [_] (log (str "played: " id ))))
    (swap! cached-sounds assoc id sound)))
