(ns chat.client.core
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs-utils.core :refer [edn-xhr]]))

(enable-console-print!)

(def app-state (atom {:messages []}))

(defn guid []
  (rand 100000))

(defn transact! [ks f]
  (swap! app-state update-in ks f))

(defn seed! []
  (reset! app-state
          {:session {:user-id 1}
           :users {1 {:id 1
                      :icon "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-08/4801271456_41230ac0b881cdf85c28_72.jpg" }
                   2 {:id 2
                      :icon "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2015-05-09/4805955000_07dc272f0a7f9de7719e_192.jpg"}}
           :threads {123 {:id 123}}
           :messages {99 {:id 99 :content "Hello?" :thread-id 123 :user-id 1}
                      98 {:id 98 :content "Hi!" :thread-id 123 :user-id 2}
                      97 {:id 97 :content "Oh, great, someone else is here." :thread-id 123 :user-id 1}
                      96 {:id 96 :content "Yep" :thread-id 123 :user-id 2}}}))

(defmulti dispatch! (fn [event data] event))

(defmethod dispatch! :new-message [_ data]
  (transact! [:messages] #(let [id (guid)]
                            (assoc % id {:id id
                                         :content (data :content)
                                         :thread-id (or (data :thread-id) (guid))
                                         :user-id (get-in @app-state [:session :user-id])}))))

(defmethod dispatch! :close-thread [_ data]
  (transact! [:threads (data :thread-id)] #(assoc % :closed true)))

(defn message-view [message owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message"}
        (dom/img #js {:className "avatar" :src (get-in @app-state [:users (message :user-id) :icon])})
        (dom/div #js {:className "content"}
          (message :content))))))

(defn new-message-view [config owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "message new"}
        (dom/textarea #js {:placeholder (config :placeholder)
                           :onKeyDown (fn [e]
                                        (when (and (= 13 e.keyCode) (= e.shiftKey false))
                                          (dispatch! :new-message {:thread-id (config :thread-id)
                                                                   :content (.. e -target -value)})
                                          (.preventDefault e)
                                          (aset (.. e -target) "value" "")))})))))
(defn thread-view [thread owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (dom/div #js {:className "close"
                      :onClick (fn [_]
                                 (dispatch! :close-thread {:thread-id (thread :id)}))} "×")
        (apply dom/div #js {:className "messages"}
          (om/build-all message-view (thread :messages)))
        (om/build new-message-view {:thread-id (thread :id) :placeholder "Reply..."})))))

(defn new-thread-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "thread"}
        (om/build new-message-view {:placeholder "Start a new conversation..."})))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (let [threads (->> (data :messages)
                         vals
                         (group-by :thread-id)
                         (map (fn [[id ms]] [id {:id id
                                                 :messages ms}]))
                         (into {})
                         (merge-with merge (data :threads))
                         vals
                         (remove (comp true? :closed)))]
        (dom/div nil
          (apply dom/div nil
            (concat (om/build-all thread-view threads)
                    [(om/build new-thread-view {})])))))))

(defn init []
  (om/root app-view app-state
           {:target (. js/document (getElementById "app"))})
  (seed!))
