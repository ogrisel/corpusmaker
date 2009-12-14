;   Copyright (c) Olivier Grisel, 2009
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

; corpusmaker - Clojure tools to build training dataset for machine learning
; based NLP algorithms out of Wikimedia dumps

(ns corpusmaker
  (:require
     [clojure.zip :as zip]
     [clojure.contrib.lazy-xml :as lxml]
     [clojure.contrib.zip-filter.xml :as zfx]
     [redis])
  (:use
     [clojure.contrib.duck-streams :only (read-lines)]))

;
; Utilities to parse DBPedia N-TRIPLES dump to load them in a redis DB to
; perform fast look up of wikilink => entity type
;

; RE to parse a line of a N-TRIPLES RDF stream to find a type relationship
; http://www.w3.org/TR/rdf-testcases/#ntriples
(def *type-pattern*
  #"<([^<]+?)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <([^<]+?)> \.")
(def *page-pattern*
  #"<([^<]+?)> <http://xmlns.com/foaf/0.1/page> <([^<]+?)> \.")

(defn collect-page
  "Parse a N-TRIPLES statement and store the [instance page] pair if match"
  [store-func statement]
  (let [[_ inst page] (re-find *page-pattern* statement)]
    (when inst (store-func page inst))))

(defn process-file [filename line-func]
  ; TODO: add start line and end line (max)
  ; TODO: add version to pass an acc explicitly and reduce instead?
  ; http://lethain.com/entry/2009/nov/15/reading-file-in-clojure/
  (count (map line-func (read-lines filename))))

(defn collect-pages-redis
  [filename]
  (redis/with-server
    {:host "127.0.0.1" :port 6379 :db 0}
    (let [store-func #(redis/set %1 %2)
          process-line #(collect-page store-func %)]
      (process-file filename process-line))))

;
; Utilities to parse a complete Wikimedia XML dump to extract sentence that
; contain token annotated with a wiki link that point to a page that matches a
; named entity with a type among the classes of the DBPedia ontology:
; Person, Organisation, Place, ...
;

; TODO: rewrite this using http://github.com/marktriggs/xml-picker-seq since
; using a zipper keeps all the parsed elements in memory which is not suitable
; for large XML chunks

(defn parse-xml
  "Zipable XML content from any common source"
  [src] (zip/xml-zip (lxml/parse-trim src)))

(defn collect-text
  "collect wikimarkup payload of a dump in seqable xml"
  [xml] (zfx/xml-> xml :page :revision :text zfx/text))


; sample timed run
; (time (dorun (collect-text (parse-xml "chunk-0001.xml"))))

