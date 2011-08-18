(ns rssminer.dbperf-test
  (:use (rssminer [database :only [import-h2-schema! use-h2-database!]])
        [clojure.tools.cli :only [cli optional required]]
        [rssminer.db.util :only [h2-query]])
  (:require [clojure.string :as str]
            [rssminer.http :as http]
            [rssminer.db.crawler :as db]
            [rssminer.time :as time]))

(def lines (str/split (slurp "test/test-rss.xml") #"\n"))
(def words (filter (complement str/blank?)
                   (str/split (slurp "test/test-rss.xml") #"\W")))

(defn gen-rss-links []
  (map (fn [line url]
         {:url url
          :title line
          :domain (http/extract-host url)
          :next_check_ts (+ (rand-int 10000)
                            (time/now-seconds))})
       (cycle lines)
       (map (fn [[a b c d]] (str "http://" a "." b "." (rand-int 500)
                                ".com/" c "/" d))
            (partition 4 (cycle words)))))

(defn do-insert [& {:keys [n path]}]
  (.delete (java.io.File. (str path ".h2.db")))
  (use-h2-database! path)
  (import-h2-schema! :trace false)
  (let [refer (first (gen-rss-links))]
    (doseq [rss (partition 10 (take n (gen-rss-links)))]
      (db/insert-crawler-links refer rss))))

(defmacro my-time [expr]
  `(let [start# (System/currentTimeMillis)
         ret# ~expr]
     {:time (- (System/currentTimeMillis) start#)
      :result ret#}))

(defn benchmark [{:keys [init times step path]}]
  (doseq [n (take times (iterate (fn [n] (* n step)) init))]
    (let [r (my-time (do-insert :n n :path path))
          inserted (-> ["select count (*) as count from crawler_links"]
                       h2-query first :count)
          time (:time r)]
      (println "\n-----" (java.util.Date.) "-----")
      (println  n "items, inserted" inserted
                (str "in " time "ms,")
                (format "%.2f per ms" (/ (double inserted) time))))
    (dotimes [i 5]
      (let [c (+ (* i 10) 80)
            r (my-time (db/fetch-crawler-links c))]
        (println "candidate"
                 (str (-> ["select count (*) as count from crawler_links
                             where next_check_ts < ?"
                           (time/now-seconds)] h2-query  first :count) ",")
                 "fetched"
                 (count (:result r))
                 (str "in " (:time r) "ms"))))))

(defn main [& args]
  "benchmark database"
  (benchmark
   (cli args
        (optional ["-i" "--init" "start" :default "10000"]
                  #(Integer/parseInt %))
        (optional ["-s" "--step" "step" :default "5"]
                  #(Integer/parseInt %))
        (optional ["-c" "--times" "step count" :default "3"]
                  #(Integer/parseInt %))
        (optional ["-p" "--path" "tmp db path"
                   :default "/tmp/h2_bench"]))))







