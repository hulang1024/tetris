(ns tetris-server.user-api-test 
  (:require
    [clj-http.cookies :refer [cookie-store]]
    [clojure.test :refer [deftest is testing]]
    [matcher-combinators.test]
    [tetris-server.api-test-util :as client :refer [setup-test-server]]))

(setup-test-server)

(deftest register
  (testing "用户注册用户名和密码格式验证"
    (let [resp (client/post "/register" {:username "ab" :password "asdfa"})]
      (is (match? {:body {:code "error"
                          :data {:errors
                                 {:username ["should be at least 3 characters"],
                                  :password ["should be at least 6 characters"]}}}}
                  resp))))

  (testing "用户注册已存在的账号直接返回特定状态码"
    (let [resp (client/post "/register" {:username "alice" :password "secret"})]
      (is (match? {:body {:code "username-exists"}}
                  resp)))))

(deftest login
  (testing "用户登录返回cookie"
    (let [resp (client/post "/login" {:username "alice" :password "secret"})]
      (is (match? {:body {:code "ok" :data {:user some?}}
                   :cookies {"ring-session" {:value some?}}}
                  resp))))

  (testing "用户登录密码错误"
    (let [resp (client/post "/login" {:username "alice" :password "wrong-pwd"})]
      (is (match? {:body {:code "failed"}} resp))
      (is (not (contains? resp :cookies)))))

  (testing "用户登录用户名不存在"
    (let [resp (client/post "/login"
                            {:username "nobody" :password "secret"})]
      (is (match? {:body {:code "failed"}} resp))
      (is (not (contains? resp :cookies))))))

(deftest whoami-and-logout
  (let [cs (cookie-store)
        login-resp (client/post "/login"
                                {:username "alice" :password "secret"}
                                {:cookie-store cs})]
    (is (match? {:body {:code "ok"}} login-resp))

    (testing "登录后获取用户信息"
      (let [resp (client/get "/whoami" nil {:cookie-store cs})]
        (is (match? {:body {:code "ok" :data {:user some?}}} resp))))

    (testing "注销清除session"
      (let [resp (client/post "/logout" nil {:cookie-store cs})]
        (is (match? {:body {:code "ok"}} resp))
        (is (not (contains? resp :cookies))))))) 
