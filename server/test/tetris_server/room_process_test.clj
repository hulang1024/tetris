(ns tetris-server.room-process-test
  (:require
    [clj-http.cookies :refer [get-cookies]]
    [clojure.test :refer [deftest is]]
    [matcher-combinators.test]
    [tetris-server.api-test-util :refer [setup-test-server]]
    [tetris-server.test-client :as client :refer [make-client]]))

(setup-test-server)

(defn is-ok [actual-resp]
  (is (match? {:body {:code "ok"}} actual-resp)))

(deftest room-process
  (let [clients [(make-client "test1" "123456")
                 (make-client "test2" "123456")
                 (make-client "test4" "123456")]
        host (first clients)]

    (run! (fn [client]
            (is-ok (client/login client))
            (println (str (:username client) ": " (get-cookies (:cs client)))))
          clients)

    (run! client/connect-socket clients)

    (is-ok (client/create-room host {:public true
                                     :game_settings {:test 1}
                                     :players-min 2
                                     :players-max 3}))
    (let [resp (client/search-rooms host nil)
          room (first (get-in resp [:body :data]))
          room-id (:id room)]
      (println (str "room-id=" room-id))
      (run! #(is-ok (client/join-room % room-id)) clients))
    (run! #(is-ok (client/toggle-ready %)) clients)
    (is-ok (client/enter-game host))
    (run! #(is-ok (client/request-start-game %)) clients)))
