(ns tetris-server.db.common
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as conn]))

(defn create-datasource [pool]
  (jdbc/with-options pool jdbc/unqualified-snake-kebab-opts))

(defn create-pool [db-config]
  (conn/->pool com.zaxxer.hikari.HikariDataSource db-config))
