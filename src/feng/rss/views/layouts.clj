(ns feng.rss.views.layouts
  (:require [net.cgrand.enlive-html :as html]))

(html/deftemplate layout "templates/layout.html" [body]
  [:#main] (html/content body))