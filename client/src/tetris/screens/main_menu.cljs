(ns tetris.screens.main-menu 
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.api :as api]
    [tetris.audio :as audio]
    [tetris.screens.menu :as menu]
    [tetris.screens.menu-overlay :refer [menu-overlay]]
    [tetris.screens.room-create :refer [room-create-overlay]]
    [tetris.screens.screen :as screen]
    [tetris.screens.screen-stack :as screen-stack]
    [tetris.screens.tip-buttons :refer [tip-buttons-overlay]]
    [tetris.screens.user :refer [display-user logic-overlay]]))

(defstyles menu-item-class [item-id]
  {:margin-top (if (= item-id :back) "1em" 0)
   :font-size "2rem"
   :color "#999"})

(defstyles menu-item-hover-class [item-id]
  {:color "#111"})

(defstyles header-class []
  {:position "absolute"
   :top 0
   :width "100%"
   :height "40px"})

(defn- ^:async join-room [room-id quick-join?]
  (let [resp (await (api/post "/room/join-room"
                              (if quick-join? {:quick_join true} {:room_id room-id})))]
    (if (= (:code resp) :ok)
      (screen-stack/push-screen! :multi-ready (:data resp))
      (js/alert "未找到房间"))))

(defmethod screen/init :main-menu [_]
  {:menu-state
   (r/atom
     (menu/make
       [(menu/item :solo nil 1)
        (menu/item :multiplayer nil 2)
        (menu/item :quick-join-room :multiplayer 1)
        (menu/item :join-room :multiplayer 2)
        (menu/item :create-room :multiplayer 3)
        #_(menu/item :list-rooms :multiplayer 4 {:screen-id :list-rooms})
        (menu/item :options nil 3)
        (menu/item :marathon :solo 1 {:screen-id :solo :mode :marathon})
        (menu/item :classic :solo 2 {:screen-id :solo :mode :classic})
        #_(menu/item :40L :solo 3 {:screen-id :solo :mode :40L})
        (menu/item :controls-options :options 1 {:screen-id :controls-options})
        (menu/item :gameplay-options :options 2 {:screen-id :gameplay-options})
        #_(menu/item :audio-options :options 3 {:screen-id :audio-options})]
       :solo))
   :login-opened false
   :room-create-opened false})

(defmethod screen/render :main-menu [this]
  [:div
   [:div {:class (header-class)}
    [display-user {:on-click #(swap! this update :login-opened not)}]
    (when (:login-opened @this)
      [logic-overlay #(swap! this assoc :login-opened false)])]
   (when (:room-create-opened @this)
     [room-create-overlay (fn [{:keys [room users]}]
                            (swap! this assoc :room-create-opened false)
                            (when room
                              (screen-stack/push-screen!
                                :multi-ready
                                {:room room :users users})))])
   [menu-overlay
    {:state (:menu-state @this)
     :on-enter (fn [{:keys [id parent] {:keys [screen-id]} :data :as item}]
                 (when (contains? #{:marathon :classic :40L} id)
                   (audio/play :gameplay))
                 (cond
                   (and (= parent :multiplayer) (= (:api-state @api/state) :offline))
                   (swap! this assoc :login-opened true)

                   (= id :quick-join-room) (join-room nil true)
                   (= id :join-room) (js/alert "TODO")
                   (= id :create-room) (swap! this assoc :room-create-opened true)

                   :else (when screen-id
                           (screen-stack/push-screen! screen-id (dissoc (:data item) :screen)))))
     :menu-item-class menu-item-class
     :menu-item-hover-class menu-item-hover-class}]
   (tip-buttons-overlay)])
