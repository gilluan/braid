(ns braid.core.client.mobile.core
  (:require
   [braid.core.client.core.events]
   [braid.core.client.core.subs]
   [braid.core.client.gateway.events]
   [braid.core.client.gateway.subs]
   [braid.core.client.mobile.auth-flow.events]
   [braid.core.client.mobile.auth-flow.routes]
   [braid.core.client.mobile.auth-flow.subs]
   [braid.core.client.mobile.views :refer [app-view]]
   [braid.core.client.router :as router]
   [braid.core.client.routes]
   [braid.core.client.state.remote-handlers]
   [braid.core.client.uploads.events]
   [braid.core.client.uploads.subs]
   [braid.core.modules :as modules]
   [re-frame.core :refer [dispatch-sync dispatch]]
   [reagent.core :as r]))

(enable-console-print!)

(defn render []
  (r/render [app-view] (. js/document (getElementById "app"))))

(defn ^:export init []
  (modules/init!)
  (dispatch-sync [:initialize-db])
  (render)
  (router/init))

(defn ^:export reload []
  (modules/init!)
  (render))
