;;   Copyright (c) Olivier Grisel, 2009
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

;; corpusmaker - Clojure tools to build training dataset for machine learning
;; based NLP algorithms out of Wikimedia dumps

(ns corpusmaker.wikipedia
  (:require
     [clojure.zip :as zip]
     [clojure.contrib.lazy-xml :as lxml]
     [clojure.contrib.zip-filter.xml :as zfx]))

;; Utilities to parse a complete Wikimedia XML dump to extract sentence that
;; contain token annotated with a wiki link that point to a page that matches a
;; named entity with a type among the classes of the DBPedia ontology:
;; Person, Organisation, Place, ...
;;
;; Also extract tokenized cleaned-up version of the text of the wikipedia
;; articles for TF-IDF / bag of words + ngrams categorization with wiki
;; categories or wiki languages as target label.

;; Parsing big dumps can be really slow if the program spends time handling
;; types dynamically
(set! *warn-on-reflection* true)

; some <!-- wiki comment --> inside the text body
(def *comment* #"<!--(.*?)-->")

; some <ref /> inside the text body
(def *ref* #"(<ref(.*?)/>|<ref(.*?)>(.*?)</ref>)")

; some {{wiki directive}} inside the text body
(def *double-curly* #"\{\{(.+?)\}\}")

; some [[Category:A given category]] inside the text body
(def *category* #"\[\[Category:(.+?)\]\]")

; some [[Known Person]] inside the text body
(def *wikilink* #"\[\[([^\|:]+?)\]\]")

; some [[Article title for Known Person|Known Person]] inside the text body
(def *qualified-wikilink* #"\[\[([^\|:]+?)\|([^\|]+?)\]\]")

; some [[w:Article title for Known Person|Known Person]] inside the text body
(def *inter-wikilink* #"\[\[([^\|:]+?):([^\|:]+?)\|([^\|]+?)\]\]")

;; TODO: rewrite this using http://github.com/marktriggs/xml-picker-seq since
;; using a zipper keeps all the parsed elements in memory which is not suitable
;; for large XML chunks

(defn parse-xml
  "Zipable XML content from any common source"
  [src] (zip/xml-zip (lxml/parse-trim src)))

(defn no-redirect?
  "Check that the page content does not forward to another article"
  [#^String page-markup]
  (-> page-markup .trim (.startsWith "#REDIRECT") not))

(defn replace-all
  "Replace all occurrences of a pattern in the given text"
  [text #^java.util.regex.Pattern pattern replacement]
  (-> pattern (.matcher text) (.replaceAll replacement)))

(defn remove-all
  "Replace all occurrences of a pattern in the given text by an empty string"
  [text #^java.util.regex.Pattern pattern]
  (replace-all text pattern ""))

(defn clean-markup
  "Remove wiki markup that does not hold annotation data"
  [#^String page-markup]
  (reduce remove-all page-markup [*comment* *double-curly* *category* *ref*]))

(defn collect-text
  "collect wikimarkup payload of a dump in seqable xml"
  [xml]
  (map clean-markup
       (filter no-redirect?
               (zfx/xml-> xml :page :revision :text zfx/text))))

(comment
  (use 'corpusmaker.wikipedia)
  (time (dorun (collect-text (parse-xml "chunk-0001.xml"))))
)

