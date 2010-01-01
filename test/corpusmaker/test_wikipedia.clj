(ns corpusmaker.test-wikipedia
  (:use
     clojure.test
     corpusmaker))

(def *sample-dumpfile* "test/enwiki-20090902-pages-articles-sample.xml")

(deftest test-collect-text
  (is 3 (count (-> *sample-dumpfile* parse-xml collect-text))))
