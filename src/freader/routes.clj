(ns freader.routes
  (:use [compojure.core :only [defroutes GET POST HEAD DELETE ANY context]]
        [ring.adapter.jetty7 :only [run-jetty]]
        [clojure.contrib.def :only [defnk]]
        (ring.middleware [keyword-params :only [wrap-keyword-params]]
                         [file-info :only [wrap-file-info]]
                         [params :only [wrap-params]]
                         [multipart-params :only [wrap-multipart-params]]
                         [file :only [wrap-file]]
                         [session :only [wrap-session]])
        (freader [middleware :only (wrap-auth
                                    wrap-content-type
                                    wrap-cache-header
                                    wrap-failsafe
                                    wrap-request-logging
                                    wrap-reload-in-dev
                                    JPOST JPUT JDELETE JGET)]
                 [database :only [use-psql-database!]]
                 [import :only [opml-import]])
        [sandbar.stateful-session :only [wrap-stateful-session]])
  (:require [freader.config :as config]
            [clojure.string :as str]
            (freader.handlers [feedreader :as freader]
                              [subscriptions :as subscription]
                              [users :as user])))

(let [views-ns '[freader.views.feedreader
                 freader.views.layouts]
      all-rss-ns (filter
                  #(re-find #"^freader" (str %)) (all-ns))
      ns-to-path (fn [clj-ns]
                   (str
                    (str/replace
                     (str/replace (str clj-ns) #"-" "_")
                     #"\." "/") ".clj"))
      src-path (fn [clj-ns]
                 {(str "src/" (ns-to-path clj-ns)) [(.getName clj-ns)]})
      src-ns-map (conj
                  (map src-path all-rss-ns)
                  {"src/templates" views-ns})]
  (def reload-meta
    (apply merge src-ns-map)))

(defroutes api-routes
  (JGET "/dashboard" [] "TODO")
  (context "/subscriptions" []
           (JGET "/overview" [] subscription/get-overview)
           (JPOST "/add" [] subscription/add-subscription)
           (JGET "/:id" [] subscription/get-subscription)
           (JPOST "/:id" [] subscription/customize-subscription)
           (JDELETE "/:id" [] subscription/unsubscribe))
  (context "/feeds/" []
           (context "/:feed-id" []
                    (JPOST "/categories" [] "TODO")
                    (JDELETE "/categories" [] "TODO")
                    (JPOST "/comments" [] "TODO")
                    (JDELETE "/comments/:comment-id" [] "TODO"))
           (JGET "/search" [] "TODO")
           (JGET "/search-ac-source" [] "TODO"))
  (JPOST "/import/opml-import" [] opml-import)
  (JGET "/export/opml-export" [] "TODO"))

(defroutes all-routes
  (GET "/" [] freader/index-page)
  (GET "/demo" [] freader/demo-page)
  (GET "/expe" [] freader/expe-page)
  (context "/login" []
           (GET "/" [] user/show-login-page)
           (POST "/" [] user/login))
  (context "/signup" []
           (GET "/" [] user/show-signup-page)
           (POST "/" [] user/signup))
  (context "/api" [] api-routes)
  (ANY "*" [] {:status 404,
               :headers {"content-type" "text/html"}
               :body "<h1>Page not found.</h1>"}))

(defn app [] (-> #'all-routes
                 wrap-keyword-params
                 wrap-multipart-params
                 wrap-params
                 wrap-auth
                 wrap-stateful-session
                 (wrap-file "public")
                 wrap-cache-header
                 wrap-file-info
                 wrap-content-type
                 wrap-request-logging
                 (wrap-reload-in-dev reload-meta)
                 wrap-failsafe))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (.stop @server)
    (reset! server nil)))

(defnk start-server [:jdbc-url "jdbc:postgresql://localhost/feedreader"
                     :db-user "postgres"
                     :db-password "123456"
                     :port 8080
                     :profile :development]
  (stop-server)
  (reset! config/env-profile profile)
  (use-psql-database! jdbc-url
                      db-user
                      db-password)
  (reset! server (run-jetty (app) {:port port :join? false})))
