(ns braid.ui.views.pages.group-settings
  (:require [reagent.ratom :include-macros true :refer-macros [reaction]]
            [reagent.core :as r]
            [chat.client.dispatcher :refer [dispatch!]]
            [chat.client.reagent-adapter :refer [subscribe]]
            [chat.client.s3 :as s3]
            [chat.client.store :as store]))

(defn intro-message-view
  [group]
  (let [new-message (r/atom "")]
    (fn [group]
      [:div.setting.intro
       [:h2 "Intro Message"]
       [:p "Current intro message"]
       [:blockquote (:intro group)]
       [:p "Set new intro"]
       [:textarea {:placeholder "New message"
                   :value @new-message
                   :on-change (fn [e] (reset! new-message (.. e -target -value)))}]
       [:button {:on-click (fn [_]
                             (dispatch! :set-intro {:group-id (group :id)
                                                    :intro @new-message})
                             (reset! new-message ""))}
        "Save"]])))

(def max-avatar-size (* 2 1024 1024))

(defn group-avatar-view
  [group]
  (let [uploading? (r/atom false)
        dragging? (r/atom false)
        start-upload (fn [group-id file-list]
                       (let [file (aget file-list 0)]
                         (if (> (.-size file) max-avatar-size)
                           (store/display-error! :avatar-set-fail "Avatar image too large")
                           (do (reset! uploading? true)
                               (s3/upload
                                 file
                                 (fn [url]
                                   (reset! uploading? false)
                                   (dispatch! :set-avatar
                                              {:group-id group-id
                                               :avatar url})))))))]
    (fn [group]
      [:div.setting.avatar {:class (when @dragging? "dragging")}
       [:h2 "Group Avatar"]
       [:div
        (if (group :avatar)
          [:img {:src (group :avatar)}]
          [:p "Avatar not set"]) ]
       [:div.upload
        (if @uploading?
          [:div
           [:p "Uploading..." [:span.uploading-indicator "\uf110"]]]
          [:div
           {:on-drag-over (fn [e]
                            (doto e (.stopPropagation) (.preventDefault))
                            (reset! dragging? true))
            :on-drag-leave (fn [_] (reset! dragging? false))
            :on-drop (fn [e]
                       (.preventDefault e)
                       (reset! dragging? false)
                       (reset! uploading? true)
                       (start-upload (group :id) (.. e -dataTransfer -files)))}
           [:label "Choose a group avatar"
            [:input {:type "file" :accept "image/*"
                     :on-change (fn [e]
                                  (start-upload (group :id)
                                                (.. e -target -files)))}]]])]])))

(defn group-settings-view
  []
  (let [group (subscribe [:active-group])
        group-id (reaction (:id @group))
        admin? (subscribe [:current-user-is-group-admin?] [group-id])]
    (fn []
      [:div.page.settings
       (if (not @admin?)
         [:h1 "Permission Denied"]
         [:div
          [:h1 (str "Settings for " (:name @group))]
          [intro-message-view @group]
          [group-avatar-view @group]])])))