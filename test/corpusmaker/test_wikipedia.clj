(ns corpusmaker.test-wikipedia
  (:use
     clojure.test
     corpusmaker.wikipedia))

(def *sample-dumpfile* "test/enwiki-20090902-pages-articles-sample.xml")

(deftest test-redirect
  (is (no-redirect? "Elves are undead Orcs."))
  (is (not (no-redirect? " #REDIRECT[[Elf (disambiguation)]]")))
  (is (not (no-redirect? " #REDIRECT [[Elf (disambiguation)]]")))
  (is (not (no-redirect? "\n#REDIRECT [[Elf (disambiguation)]]")))
  (is (not (no-redirect? "#REDIRECT [[Elf (disambiguation)]]"))))

(deftest test-clean-markup
  (is (= "Some text" (clean-markup "{{directive 1}}Some{{directive 2}} text")))
  (is (= "Some text" (clean-markup "<!-- this is a comment -->Some text")))
  (is (= "Some text" (clean-markup "[[Category:Test document]]Some text"))))

(deftest test-parse-sample-dump
  (let [articles (-> *sample-dumpfile* parse-xml collect-text)]
    (is (= 2 (count articles)))))
    ;(is (= "" (first articles)))))

