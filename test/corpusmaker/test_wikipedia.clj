(ns corpusmaker.test-wikipedia
  (:use
     clojure.test
     corpusmaker))

(def *sample-dumpfile* "test/enwiki-20090902-pages-articles-sample.xml")

(deftest test-redirect
  (is (no-redirect? "Elves are undead Orcs."))
  (is (not (no-redirect? " #REDIRECT[[Elf (disambiguation)]]")))
  (is (not (no-redirect? " #REDIRECT [[Elf (disambiguation)]]")))
  (is (not (no-redirect? "\n#REDIRECT [[Elf (disambiguation)]]")))
  (is (not (no-redirect? "#REDIRECT [[Elf (disambiguation)]]"))))

(deftest test-parse-and-filter-redirects
  (is (= 2 (count (-> *sample-dumpfile* parse-xml collect-text)))))

