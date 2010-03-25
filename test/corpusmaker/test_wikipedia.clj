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
    (let [[text links categories] (parse-markup (first articles))]
      (is (= #{"Anarchism" "Political culture"
               "Social theories"
               "Political ideologies"} categories))
      (is (= 465 (count links)))
      (is (= "political philosophy" (:label (first links)))))))

(deftest test-tokenize
  (is (=
    '("term1" "term2" "term3")
    (tokenize-markup "term1 term2 term3")))
  (is (=
    '("This" "is" "an" "internal" "link" "to" "another" "Wikipedia" "article")
    (tokenize-markup
      "This is an [[internal link]] to another *Wikipedia* article."))))

(deftest test-tokenize-wikipedia
  (let [articles (collect-text *sample-dumpfile*)
        article-tokens (map tokenize-markup articles)]
    (is (=
      '("pp" "move" "indef" "Anarchism" "sidebar" "Anarchism" "is"
        "a" "political" "philosophy" "encompassing" "anarchist")
      (take 12 (first article-tokens))))
        (is (=
      '("NOTES" "1" "Please" "follow" "the" "Wikipedia" "style"
        "guidelines" "for" "editing" "medical" "articles")
      (take 12 (second article-tokens))))))

(deftest test-ngrams
  (is (=
    (list
      '(0 1 2)
      '(1 2 3)
      '(2 3 4))
    (ngrams 3 (range 5))))
  (is (=
    (list
      '("This" "is" "a")
      '("is" "a" "test"))
    (ngrams 3 (tokenize-markup "This [[is]] a test."))))
  (is (=
    (list
      '(nil nil "This")
      '(nil "This" "is")
      '("This" "is" "a")
      '("is" "a" "test")
      '("a" "test" nil)
      '("test" nil nil))
    (padded-ngrams 3 (tokenize-markup "This [[is]] a test.")))))

(deftest test-trigrams-text
  (is (=
    '("this is a"
      "is a very"
      "a very interesting"
      "very interesting test")
    (trigrams-text "This is a very interesting test."))))
