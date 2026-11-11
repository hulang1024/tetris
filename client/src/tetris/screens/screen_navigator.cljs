(ns tetris.screens.screen-navigator
  (:require
    [reagent.core :as r]
    [tetris.audio :as audio]))

(def Screen
  [:map
   [:name :keyword]                      ; 唯一标识名称，优化渲染性能需要
   [:build fn?]                          ; 渲染/构建UI
   [:on-entering {:optional true} fn?]   ; 当首次进入
   [:on-exiting {:optional true} fn?]    ; 当退出
   [:on-suspending {:optional true} fn?] ; 当push另一个screen
   [:on-resuming {:optional true} fn?]   ; 当pop后
   [:on-keydown {:optional true} fn?]    ; 当按键按下
   [:on-update {:optional true} fn?]])   ; 动画帧更新

(def ^:private state (r/atom {:stack []}))

(defn- push-screen [state screen]
  (update state :stack conj screen))

(defn- pop-screen [state]
  (update state :stack pop))

(defn- replace-screen [state screen]
  (update state :stack #(conj (pop %) screen)))

(defn active-screen []
  (peek (:stack @state)))

(defn- trigger-callback [screen callback-key & args]
  (when-let [cb (callback-key screen)]
    (apply cb screen args)))

(defn- load-screen! [screen cb]
  (let [load! (fn [screen]
                (cb screen)
                (trigger-callback screen :on-entering))]
    (if (instance? js/Promise screen)
      (.then screen load!)
      (load! screen))))

(defn push-screen!
  {:malli/schema [:=> [:cat Screen] :any]}
  [screen]
  (trigger-callback (active-screen) :on-suspending)
  (load-screen! screen #(swap! state push-screen %)))

(defn pop-screen! []
  (when (not (empty? (:stack (pop-screen @state))))
    (trigger-callback (active-screen) :on-exiting)
    (swap! state pop-screen)
    (trigger-callback (active-screen) :on-resuming)))

(defn replace-screen!
  {:malli/schema [:=> [:cat Screen] :any]}
  [screen]
  (trigger-callback (active-screen) :on-exiting)
  (load-screen! screen #(swap! state replace-screen %)))

(defn on-keydown [event]
  (when (.-ctrlKey event)
    (.preventDefault event))
  (when-let [screen (active-screen)]
    (let [handled? (trigger-callback screen :on-keydown event)]
      (when (and (not handled?)
                 (= (.-code event) "Escape")
                 (not (.-repeat event))
                 (= (.. event -target -tagName) "BODY"))
        (audio/play :screen-back)
        (pop-screen!)))))

