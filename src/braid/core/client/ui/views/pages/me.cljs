(ns braid.core.client.ui.views.pages.me
  (:require
   [braid.core.client.routes :as routes]))

(defn me-page-view
  []
  [:div.page.me
   [:div.title "Me!"]
   [:div.content
    [:p "Placeholder page for group-related profile settings"]
    [:p
     [:a {:href (routes/system-page-path {:page-id "global-settings"})}
      "Go to Global Settings"]]]])
