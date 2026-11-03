(ns tetris-server.room-manage-test
  (:require [clojure.test :refer [deftest is testing]]
            [matcher-combinators.test :refer [match?]]
            [tetris-server.handler.room :as room]
            [tetris-server.handler.room-manager :as manager]))

(def one-room (room/make-room {:id "1"
                               :host 1
                               :players-min 2
                               :players-max 99}))

(deftest room-manage
  (testing "可重复"
    (is (match? {"1" (assoc one-room :players [{:id 1 :state :not-ready}])}
                (-> {}
                    (manager/put-room one-room)
                    (manager/put-room one-room))))))

(deftest join-room
  (testing "可重复/最大人数"
    (let [one-room (assoc one-room :players-max 2)]
      (is (match? (assoc one-room
                         :state :ready
                         :players [{:id 1 :state :not-ready}
                                   {:id 2 :state :not-ready}])
                  (-> one-room
                      (room/join 2)
                      (room/join 2)
                      (room/join 3)))))))

(deftest leave-room
  (testing "可重复"
    (is (match? (assoc one-room :players [{:id 1 :state :not-ready}])
                (-> one-room
                    (room/join 2)
                    (room/leave 2)
                    (room/leave 2)))))

  (testing "房主离开后自动更新房主"
    (is (match? (assoc one-room
                       :host 2
                       :players [{:id 2 :state :not-ready}])
                (-> one-room
                    (room/join 2)
                    (room/leave 1)))))

  (testing "玩家全部离开后空房间自动删除"
    (is (match? {:players []}
                (-> one-room
                    (room/join 2)
                    (room/leave 1)
                    (room/leave 2))))))

(deftest toggle-ready
  (testing "准备"
    (is (match? (assoc one-room :players [{:id 1 :state :not-ready}])
                (-> one-room
                    (room/toggle-ready 1)
                    (room/toggle-ready 1))))))

(deftest start-game
  (testing "人数小于设置最小人数则失败"
    (let [one-room (assoc one-room :players-min 2)]
      (is (match? one-room
                  (-> one-room
                      (room/enter-game false)
                      (room/enter-game false)
                      (room/enter-game true))))))

  (testing "非强制开始时如非全部已准备则失败"
    (is (match? (room/join one-room 2)
                (-> one-room
                    (room/join 2)
                    (room/enter-game false)))))

  (testing "强制开始忽略准备状态"
    (is (match? (assoc (room/join one-room 2) :state :inprogress)
                (-> one-room
                    (room/join 2)
                    (room/enter-game true)))))

  (testing "普通情况/可重复开始"
    (is (match? (assoc (-> one-room
                           (room/join 2)
                           (room/toggle-ready 1)
                           (room/toggle-ready 2))
                       :state :inprogress)
                (-> one-room
                    (room/join 2)
                    (room/toggle-ready 1)
                    (room/toggle-ready 2)
                    (room/enter-game true)
                    (room/enter-game true))))))
