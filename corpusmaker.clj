;;   Copyright (c) Olivier Grisel, 2009
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

;; corpusmaker - Clojure tools to build training dataset for machine learning
;; based NLP algorithms out of Wikimedia dumps

(ns corpusmaker
  (:require
     [clojure.zip :as zip]
     [clojure.contrib.lazy-xml :as lxml]
     [clojure.contrib.zip-filter.xml :as zfx]
     [redis])
  (:use
     [clojure.contrib.duck-streams :only (read-lines)]))

;;
;; Utilities to parse DBPedia N-TRIPLES dump to load them in a redis DB to
;; perform fast look up of wikilink => entity type
;;

;; RE to parse a line of a N-TRIPLES RDF stream to find a type relationship
;; http://www.w3.org/TR/rdf-testcases/#ntriples
(def *type-pattern*
  #"<([^<]+?)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <([^<]+?)> \.")
(def *page-pattern*
  #"<([^<]+?)> <http://xmlns.com/foaf/0.1/page> <([^<]+?)> \.")

(def *pages-db* "entities-pages")
(def *types-db* "pages-types")
(def *wikipage-filename* "wikipage_en.nt")
(def *instancetype-filename* "instancetype_en.nt")

(defn process-file [filename line-func]
  ; TODO: add start line and end line (max)
  ; TODO: add version to pass an acc explicitly and reduce instead?
  ; http://lethain.com/entry/2009/nov/15/reading-file-in-clojure/
  (count (map line-func (read-lines filename))))

(defn collect-page
  "Parse a N-TRIPLES statement and store the [page entity] pair if match"
  [statement]
  (let [[_ entity page] (re-find *page-pattern* statement)]
    (when entity
      (redis/set entity page))))

(defn collect-entities-to-pages
  "Parse a N-TRIPLES file to extract and store entity - page relationships"
  [filename server-params]
  (redis/with-server
    server-params
    (redis/select *pages-db*)
    (process-file filename collect-page)))

(defn join-page-type
  "Parse a N-TRIPLES statement and store the [page type] pair if match"
  [statement]
  (let [[_ entity type_] (re-find *type-pattern* statement)]
    (when entity
      (redis/select *pages-db*)
      (when-let [page (redis/get entity)]
        (redis/select *types-db*)
        (redis/set page type_)))))

(defn collect-pages-to-types
  "Parse a N-TRIPLES file to extract types to be joined with page db"
  [filename server-params]
  (redis/with-server
    server-params
    (process-file filename join-page-type)))

(defn build-page-type-db
  "Build a redis db from dbpedia NT dumps to be found in folder

  Only instances-types_en.nt and wikipage_en.nt are required."
  [folder server-params]
  (collect-entities-to-pages
    (str folder "/" *wikipage-filename*)
    server-params)
  (collect-pages-to-types
    (str folder "/" *instancetype-filename*)
    server-params))

(comment
  (use 'corpusmaker)
  (time (build-page-type-db "/home/ogrisel/data/dbpedia" {}))
)
;;
;; Utilities to parse a complete Wikimedia XML dump to extract sentence that
;; contain token annotated with a wiki link that point to a page that matches a
;; named entity with a type among the classes of the DBPedia ontology:
;; Person, Organisation, Place, ...
;;

;; TODO: rewrite this using http://github.com/marktriggs/xml-picker-seq since
;; using a zipper keeps all the parsed elements in memory which is not suitable
;; for large XML chunks

(defn parse-xml
  "Zipable XML content from any common source"
  [src] (zip/xml-zip (lxml/parse-trim src)))

(defn collect-text
  "collect wikimarkup payload of a dump in seqable xml"
  [xml] (zfx/xml-> xml :page :revision :text zfx/text))


;; sample timed run
;; (time (dorun (collect-text (parse-xml "chunk-0001.xml"))))

