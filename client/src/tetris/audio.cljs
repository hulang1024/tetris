(ns tetris.audio
  (:require ["howler" :as howler]))

(def ^:private cached-sounds (atom {}))

(defn sound [id] (id @cached-sounds))

(defn play [id]
  (.play (sound (keyword id))))

(defn- log [s]
  (tap> (str "aduio - " s)))

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
  (for [name ["menu-hover"
              "menu-enter"
              "menu-back"
              "gameplay"
              "screen-back"]
        :let [id (keyword name)
              src (str "assets/ui/" name ".mp3")]]
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
