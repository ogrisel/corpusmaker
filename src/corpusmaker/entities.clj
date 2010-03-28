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
    [clojure.contrib.duck-streams :only (read-lines)])
  (:require
    (cascading.clojure [api :as c]))
  (:import
    cascading.operation.regex.RegexParser
    cascading.pipe.Each
    cascading.tuple.Fields))

;; Utilities to parse DBPedia N-TRIPLES and make it easier to
;; process them in cascading flows
;(set! *warn-on-reflection* true)

;; RE to parse a line of a N-TRIPLES RDF stream to find a type relationship
;; http://www.w3.org/TR/rdf-testcases/#ntriples

(def *wikipage-filename* "wikipage_en.nt")
(def *instancetype-filename* "instancetype_en.nt")
(def *longabstract-filename* "longabstract_en.nt")

(def *uri-uri-uri-pattern* #"<([^<]+?)> <([^<]+?)> <([^<]+?)> \.")
(def *uri-uri-literal-pattern* #"<([^<]+?)> <([^<]+?)> \"(.*)\"@(\w\w) \.")

(def *owl-thing* "http://www.w3.org/2002/07/owl#Thing")
(def *rdf-type* "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
(def *foaf-page* "http://xmlns.com/foaf/0.1/page")

(defn url-decode
  [url]
  (java.net.URLDecoder/decode url))

(defn extract-property
  "Extract entity propety value (object is a literal)"
  [line]
  (let [[r p v l] (rest (re-find *uri-uri-literal-pattern* line))]
    [(url-decode r) (url-decode p) (.replace v "\\\"" "\"") l]))

(defn extract-relation
  "Extract entities relationsips (subject predicate and object are URIs)"
  [line]
  (map url-decode (rest (re-find *uri-uri-uri-pattern* line))))

(defn not-owl-thing?
  "Helper to filter out owl:Thing lines"
  [type]
  (not= type *owl-thing*))

(defn serialize-tuple
  "Helper to serializa tuples of clojure values into TextLine schemes"
  [& tuple-elems]
  (pr-str (vec tuple-elems)))

(defn join-abstract-type
  "Execute a flow to join type and abstract info"
  [abstract-file type-file out-file]
  (let [
    ;; first pipe to extract abstracts data
    p-abstracts (->
      (c/pipe "abstracts")
      (c/map #'extract-property
        :< "line"
        :fn> ["resource" "p_abstract" "abstract" "lang"]
        :> ["resource" "abstract"]))

    ;; second pipe to extract typing info
    p-types (->
      (c/pipe "types")
      (c/map #'extract-relation
        :< "line"
        :fn> ["resource" "p_type" "type"]
        :> ["offset" "resource" "type"])
      (c/filter #'not-owl-thing? :< "type") ; remove owl:Thing lines
      (c/group-by "resource" "offset") ; preserve file order using offset
      (c/first "type")) ; select the most generic type (trust file order)

    ;; join the first two pipes on resource URI as key
    joined (->
      [p-abstracts p-types]
      (c/inner-join
        [["resource"] ["resource"]]
        ["resource1" "abstract" "resource2" "type"])
      (c/select ["resource1" "abstract" "type"]))

    ;; map plug in the input files, results serialization and execute
    flow (c/flow
      {"abstracts" (c/lfs-tap (c/text-line "line") abstract-file)
       "types" (c/lfs-tap (c/text-line ["offset" "line"]) type-file)}
      (c/lfs-tap (c/text-line) out-file)
      (c/map joined #'serialize-tuple :< Fields/ALL :fn> "line" :> "line"))]
    (c/exec flow)))
