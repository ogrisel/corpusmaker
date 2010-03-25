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
  (:use
     [clojure.contrib.duck-streams :only (read-lines)]))

;; Utilities to parse DBPedia N-TRIPLES and make it easier to
;; process them in cascading flows
;(set! *warn-on-reflection* true)

;; RE to parse a line of a N-TRIPLES RDF stream to find a type relationship
;; http://www.w3.org/TR/rdf-testcases/#ntriples


(def *wikipage-filename* "wikipage_en.nt")
(def *type-pattern*
  #"<([^<]+?)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <([^<]+?)> \.")

(def *instancetype-filename* "instancetype_en.nt")
(def *page-pattern*
  #"<([^<]+?)> <http://xmlns.com/foaf/0.1/page> <([^<]+?)> \.")

(def *uri-uri-literal-pattern*
  #"<([^<]+?)> <([^<]+?)> \"(.*)\"@(\w\w) \.")

(def *owl-Thing* "http://www.w3.org/2002/07/owl#Thing")


