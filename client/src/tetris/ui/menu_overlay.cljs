(ns tetris.ui.menu-overlay
  (:require-macros [cljss.core])
  (:require
    [cljss.core :as css :refer-macros [defstyles]]
    [reagent.core :as r]
    [tetris.i18n.core :as i18n]
    [tetris.audio :as audio]
    [tetris.ui.menu :as menu]))

(defstyles base-item-class [item-id]
  {:padding "0.5em 0"
   :font-size "1.5rem"
   :text-align "center"
   :user-select "none"
   :color "#999"})

(defstyles base-item-hover-class [item-id]
  {:color "#111"
   :text-shadow "0 0 0.2em #fee"})

(defn- handle-select [state on-select on-update]
  (let [item-id (:hover state)
        item (or (menu/find-item state item-id) {:id :back})
        trigger-select (fn [new-state item]
                         (when on-select (on-select item new-state))
                         (on-update new-state))]
    (cond
      (= item-id :back)
      (do (audio/play :menu-back)
          (on-update (menu/back-to-parent state) item))

      (menu/menu? state item-id)
      (do (audio/play :menu-select)
          (trigger-select (menu/select-hover-menu state) item))

      :else (trigger-select state item))))

(defn- handle-hover [state on-update]
  (audio/play :menu-hover)
  (on-update state))

(defn- handle-keydown [state event on-select on-update]
  (let [key (.-code event)
        trigger-hover (fn [new-state]
                        (audio/play :menu-hover)
                        (on-update new-state))]
    (case key
      "ArrowDown" (trigger-hover (menu/move-hover state +1))
      "ArrowUp" (trigger-hover (menu/move-hover state -1))
      "Escape" (do (when (:active state)
                     (audio/play :menu-back))
                   (on-update (menu/back-to-parent state)))
      "Enter" (handle-select state on-select on-update)
      nil)))

(defn menu-overlay [{:keys [key
                            state
                            menu-items
                            default-hover
                            menu-class
                            menu-item-class
                            menu-item-hover-class
                            on-select]}]
  (r/with-let [state (or state (r/atom (menu/make menu-items default-hover)))
               update-state #(reset! state %)
               keydown-event-listener (fn [event]
                                        (when-not (.-repeat event) 
                                          (handle-keydown
                                            @state
                                            event
                                            on-select update-state)))]
    (js/document.addEventListener "keydown" keydown-event-listener)
    [:div {:key (or key "menu")
           :class ["menu-overlay"
                   (when menu-class (menu-class))]}
     (map
       (fn [item]
         [:div
          {:key (:id item)
           :class (into
                    [(base-item-class (:id item))
                     (when menu-item-class
                       (menu-item-class (:id item)))]
                    (when (= (:hover @state) (:id item))
                      [(base-item-hover-class (:id item))
                       (when menu-item-hover-class
                         (menu-item-hover-class (:id item)))]))
           :on-mouse-over #(handle-hover
                             (menu/change-hover @state (:id item))
                             update-state)
           :on-click #(handle-select @state on-select update-state)}
          (i18n/text [:menu (:id item)])])
       (menu/show-active-menu-items @state))]
    (finally
      (js/document.removeEventListener "keydown" keydown-event-listener))))
