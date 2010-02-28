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
  (is (= "Some - text" (clean-markup "Some{{ndash}}text")))
  (is (= "{{lang1}}{{lang2}}" (clean-markup "{{lang1}}\n{{lang2}}\n"))))

;; TODO: write unit tests for parse-markup

(deftest test-parse-sample-dump
  (let [articles (collect-text *sample-dumpfile*)]
    (is (= 2 (count articles)))
    (is (= "{{pp-move-indef}}{{Anarchism s" (.substring (first articles) 0 30 )))
    (let [anarchism (parse-markup (first articles))]
      (is (= #{"Anarchism" "Political culture"
               "Social theories"
               "Political ideologies"} (:categories anarchism)))
      (is (= 465 (count (:links anarchism))))
      (is (= "political philosophy" (:label (first (:links anarchism))))))))

(deftest test-tokenize
  (is (=
    (list "term1" "term2" "term3")
    (tokenize "term1 term2 term3")))
  (is (=
    (list
      "This" "is" "an" "internal" "link" "to" "another" "Wikipedia" "article")
    (tokenize
      "This is an [[internal link]] to another *Wikipedia* article."))))

(deftest test-ngrams
  (is (=
    (list (list 0 1 2) (list 1 2 3) (list 2 3 4))
    (ngrams 3 (range 5))))
  (is (=
    (list
      (list "This" "is" "a")
      (list "is" "a" "test"))
    (ngrams 3 (tokenize "This [[is]] a test."))))
  (is (=
    (list
      (list nil nil "This")
      (list nil "This" "is")
      (list "This" "is" "a")
      (list "is" "a" "test")
      (list "a" "test" nil)
      (list "test" nil nil))
    (padded-ngrams 3 (tokenize "This [[is]] a test.")))))