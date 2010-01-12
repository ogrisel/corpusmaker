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
  (:use clojure.contrib.duck-streams)
  (:import java.util.regex.Pattern
     javax.xml.stream.XMLInputFactory
     javax.xml.stream.XMLStreamReader
     javax.xml.stream.XMLStreamConstants
     corpusmaker.CorpusMakerTextConverter))

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


; remove new lines after links or templates that are not to be rendered in the
; text version
(def *spurious-eol* #"(?m)(\}\}|\]\])\n")

; the {{ndash}} ubiquitous template
(def *ndash* #"\{\{ndash\}\}")

(def *replacements*
  [{:pattern *ndash* :replacement " - "}
  {:pattern *spurious-eol* :replacement "$1"}])

;; TODO: rewrite this using http://github.com/marktriggs/xml-picker-seq since
;; using a zipper keeps all the parsed elements in memory which is not suitable
;; for large XML chunks

(defn no-redirect?
  "Check that the page content does not forward to another article"
  [#^String page-markup]
  (-> page-markup .trim (.startsWith "#REDIRECT") not))

(defn replace-all
  "Replace all occurrences of a pattern in the given text"
  [text {#^Pattern pattern :pattern replacement :replacement}]
  (-> pattern (.matcher text) (.replaceAll replacement)))

(defn remove-all
  "Replace all occurrences of a pattern in the given text by an empty string"
  [text #^Pattern pattern]
  (replace-all text {:pattern pattern :replacement ""}))

(defn clean-markup
  "Proprocess wiki markup to remove unneeded parts"
  [#^String page]
  (reduce replace-all (.trim page) *replacements*))

(defn parser-to-text-seq
  "Extract raw wikimarkup from the text tags encountered by an XML stream parser"
  [#^XMLStreamReader parser]
  (let [event (.next parser)]
    (if (== event XMLStreamConstants/END_DOCUMENT)
      (.close parser) ; returns nil
      (if (== event XMLStreamConstants/START_ELEMENT)
        (if (= (.getLocalName parser) "text")
          (cons (.getElementText parser) (parser-to-text-seq parser))
          (recur parser))
        (recur parser)))))

(defn collect-raw-text
  "collect wikimarkup payload of a dump in seqable xml"
  [dumpfile]
  (let [factory (XMLInputFactory/newInstance)
        is (java.io.FileInputStream. (file-str dumpfile))
        parser (.createXMLStreamReader factory (reader is))]
    (parser-to-text-seq parser)))

(defn collect-text
  "collect and preprocess wikimarkup payload of a dump in seqable xml"
  [dumpfile]
  (map clean-markup (filter no-redirect? (collect-raw-text dumpfile))))

(defn parse-markup
  "Remove wikimarkup while collecting links to entities and categories"
  [page-markup]
  (let [model (CorpusMakerTextConverter/newWikiModel)
        converter (CorpusMakerTextConverter.)
        text (.render model converter page-markup)]
    {:text text :categories (-> model (.getCategories) (.keySet) set)
     :links (vec (map #(hash-map
                         :label (.label %) :start (.start %) :end (.end %))
                      (.getWikiLinks converter)))}))

(comment
  (use 'corpusmaker.wikipedia)
  (time (dorun (collect-text "chunk-0001.xml")))
)

