;;   Copyright (c) Olivier Grisel, 2009
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

;; corpusmaker - Clojure tools to build training dataset for machine learning
;; based NLP algorithms out of Wikimedia dumps

(ns corpusmaker.entities
  (:require
     [clojure.zip :as zip]
     [clojure.contrib.lazy-xml :as lxml]
     [clojure.contrib.zip-filter.xml :as zfx]
     [redis])
  (:use
     [clojure.contrib.duck-streams :only (read-lines)]))

;; Utilities to parse DBPedia N-TRIPLES dump to load them in a redis DB to
;; perform fast look up of wikilink => entity type

(set! *warn-on-reflection* true)

;; RE to parse a line of a N-TRIPLES RDF stream to find a type relationship
;; http://www.w3.org/TR/rdf-testcases/#ntriples
(def *type-pattern*
  #"<([^<]+?)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <([^<]+?)> \.")
(def *page-pattern*
  #"<([^<]+?)> <http://xmlns.com/foaf/0.1/page> <([^<]+?)> \.")

(def *owl-Thing* "http://www.w3.org/2002/07/owl#Thing")
(def *pages-db* "entities-pages")
(def *types-db* "pages-types")
(def *wikipage-filename* "wikipage_en.nt")
(def *instancetype-filename* "instancetype_en.nt")

(defn process-file [filename line-func]
  "Apply line-func to each line of filename and return the number of lines"
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
  "Parse a N-TRIPLES statement and store the [page list(types)] pair if match"
  [statement]
  (let [[_ entity type_] (re-find *type-pattern* statement)]
    ; all instances are of type Thing hence not interesting for our use case
    (when (and entity (not= type_ *owl-Thing*))
      (redis/select *pages-db*)
      (when-let [page (redis/get entity)]
        (redis/select *types-db*)
        (redis/rpush page type_)))))

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
  (use 'corpusmaker.entities)
  (time (build-page-type-db "/home/ogrisel/data/dbpedia" {}))
)

