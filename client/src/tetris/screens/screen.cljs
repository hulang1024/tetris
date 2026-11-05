(ns tetris.screens.screen 
  (:require
    [reagent.core :as r]))

(def get-id (comp :id deref))

;; 初始化状态（副作用应该在on-entering中）
(defmulti init identity)
;; 渲染UI
(defmulti build get-id)
;; 当首次进入
(defmulti on-entering get-id)
;; 当退出
(defmulti on-exiting get-id)
;; 当push另一个screen
(defmulti on-suspending get-id)
;; 当pop后
(defmulti on-resuming get-id)
;; 按键
(defmulti on-keydown get-id)
;; 动画更新 
(defmulti on-update get-id)

;;;; default
(defmethod init :default [id & _args] (r/atom {:id id}))

(defmethod build :default [screen]
  [:div (str "id: "(name (get-id screen)))])

(defmethod on-entering :default [_] nil)

(defmethod on-exiting :default [_] nil)

(defmethod on-suspending :default [_] nil)

(defmethod on-resuming :default [_] nil)

(defmethod on-keydown :default [_] false)

(defmethod on-update :default [_] nil)
