(ns rssminer.fetcher
  (:use [rssminer.util :only [md5-sum assoc-if start-tasks]]
        [clojure.tools.logging :only [error trace]]
        [rssminer.db.crawler :only [update-rss-link fetch-rss-links]]
        [rssminer.parser :only [parse-feed]])
  (:require [rssminer.db.feed :as db]
            [rssminer.http :as http]
            [rssminer.config :as conf])
  (:import [java.util Queue LinkedList]))

(def ^Queue queue (LinkedList.))

(defn fetch-rss
  [{:keys [id url check_interval last_modified last_md5] :as link}]
  (let [{:keys [status headers body]} (http/get url
                                                :last_modified last_modified)
        html (when body (try (slurp body)
                             (catch Exception e
                               (error e url))))
        md5 (when html (md5-sum html))
        feeds (when html (parse-feed html))
        next-check (conf/next-check check_interval
                                    (and (= 200 status) (not= md5 last_md5)))]
    (trace status url (str "(" (-> feeds :entries count) " feeds)"))
    (update-rss-link id (assoc-if next-check
                                  :last_md5 md5
                                  :last_modified (:last_modified headers)))
    (when feeds (db/save-feeds feeds id nil))))

(defn get-next-link []
  (locking queue
    (if (.peek queue) ;; has element?
      (.poll queue)   ;; retrieves and removes
      (let [links (fetch-rss-links conf/fetch-size)]
        (trace "fetch" (count links) "rss links from h2")
        (when (seq links)
          (doseq [link links]
            (.offer queue link))
          (get-next-link))))))

(defn start-fetcher [& {:keys [threads]}]
  (start-tasks get-next-link fetch-rss "fetcher"
                 (or threads conf/crawler-threads-count)))