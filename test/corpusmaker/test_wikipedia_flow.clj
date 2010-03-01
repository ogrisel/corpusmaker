(ns corpusmaker.test-wikipedia-flow
  (:use clojure.test
    cascading.clojure.io)
  (:require (cascading.clojure [api :as c])
    (clojure.contrib [duck-streams :as ds])
    (clojure.contrib [java-utils :as ju])
    (clj-json [core :as json])
    (corpusmaker.cascading [wikipedia :as ccw])))

(def *sample-dumpfile* "test/enwiki-20090902-pages-articles-sample.xml")

(defn up-title
  {:fields ["up-title" "start-markup"]}
  [title markup]
  [(.toUpperCase title) (-> markup (.replace "\n" " ") (.substring 0 30))])

(deftest test-wikipedia-source
  ;; parse wikipedia XML dump to extract title and markup into transformed text lines
  (with-log-level :warn
    (with-tmp-files [sink-path (temp-path "corpusmaker-test-sink")]
      ;; create a flow from wikipedia raw format to json map lines
      (let [flow
            (c/flow
              {"wikipedia" (ccw/wikipedia-tap *sample-dumpfile*)}
              (c/lfs-tap (c/text-line ["up-title" "start-markup"]) sink-path)
              (-> (c/pipe "wikipedia") (c/map ["title" "markup"] #'up-title)))]
        ;; run the flow
        (c/exec flow)
        ;; parse check the output text file contents in the sink folder
        (let [output-lines (ds/read-lines (ju/file sink-path "part-00000"))]
          (is (= 4 (.size output-lines)))
          (is (= "ACCESSIBLECOMPUTING\t#REDIRECT [[Computer accessibi" (nth output-lines 0)))
          (is (= "ANARCHISM\t{{pp-move-indef}} {{Anarchism " (nth output-lines 1)))
          (is (= "AFGHANISTANHISTORY\t#REDIRECT [[History of Afghani" (nth output-lines 2)))
          (is (= "AUTISM\t<!-- NOTES: 1) Please follow t" (nth output-lines 3))))))))
