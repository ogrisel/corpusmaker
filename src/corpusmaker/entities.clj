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
    [clojure.contrib.duck-streams :only (read-lines file-str)])
  (:require
    (cascading.clojure [api :as c]))
  (:import
    cascading.operation.regex.RegexParser
    cascading.pipe.Each
    cascading.tuple.Fields
    com.hp.hpl.jena.rdf.model.ModelFactory
    com.hp.hpl.jena.tdb.TDBFactory
    org.apache.lucene.analysis.standard.StandardAnalyzer
    org.apache.lucene.util.Version
    org.apache.lucene.store.FSDirectory
    org.apache.lucene.index.IndexWriter
    org.apache.lucene.index.IndexWriter$MaxFieldLength))

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
  [#^String url]
  (java.net.URLDecoder/decode url))

;; TODO: the following is buggy: unicode ASCII encoded symbols needs to be taken
;; care of (see the Jena N-TRIPLE literal parsing code for instance)
(defn extract-property
  "Extract entity propety value (object is a literal)"
  [#^String line]
  (let [[r p #^String v l] (rest (re-find *uri-uri-literal-pattern* line))]
    [r p (.replace v "\\\"" "\"") l]))

(defn extract-relation
  "Extract entities relationsips (subject predicate and object are URIs)"
  [#^String line]
  (rest (re-find *uri-uri-uri-pattern* line)))

(defn not-owl-thing?
  "Helper to filter out owl:Thing lines"
  [#^String type]
  (not= type *owl-thing*))

(defn serialize-tuple
  "Helper to serialize tuples of clojure values into TextLine schemes"
  [& tuple-elems]
  (pr-str (vec tuple-elems)))

;; This is useless since DBpedia can fit in memory and its better to use jena + lucene
;; directly
(defn join-abstract-type
  "Execute a flow to join type and abstract info"
  [abstract-file type-file out-folder]
  (let [
    ;; first pipe to extract abstracts data
    abstracts-pipe (->
      (c/pipe "abstracts")
      (c/map #'extract-property
        :< "line"
        :fn> ["resource" "p_abstract" "abstract" "lang"]
        :> ["resource" "abstract"]))

    ;; second pipe to extract typing info
    types-pipe (->
      (c/pipe "types")
      (c/map #'extract-relation
        :< "line"
        :fn> ["resource" "p_type" "type"]
        :> ["offset" "resource" "type"])
      (c/filter #'not-owl-thing? :< "type") ; remove owl:Thing lines
      (c/group-by "resource" "offset") ; preserve file order using offset
      (c/first "type")) ; select the most generic type (trust file order)

    ;; join the first two pipes on resource URI as key
    joined-pipe (->
      [abstracts-pipe types-pipe]
      (c/inner-join
        [["resource"] ["resource"]]
        ["resource1" "abstract" "resource2" "type"])
      (c/select ["resource1" "abstract" "type"]))

    ;; map plug in the input files, results serialization and execute
    flow (c/flow
      {"abstracts" (c/lfs-tap (c/text-line "line") abstract-file)
       "types" (c/lfs-tap (c/text-line ["offset" "line"]) type-file)}
      (c/lfs-tap (c/text-line) out-folder)
      (c/map joined-pipe #'serialize-tuple :< Fields/ALL :fn> "line" :> "line"))]
    (c/exec flow)))

(defn replace-redirect
  "Replace by redirect if any"
  [#^String orig #^String redirect]
  (if (nil? redirect) orig redirect))

(defn count-incoming
  "Count the number of incoming links to a resource for popularity ranking"
  ([link-file redirect-file out-folder]
    (count-incoming link-file redirect-file out-folder false))
  ([link-file redirect-file out-folder to-clj]
    (let [
      link-pipe (->
        (c/pipe "links")
        (c/map #'extract-relation
          :< "line" :fn> ["from" "link-prop" "to"] :> ["from" "to"]))

      redirect-pipe (->
        (c/pipe "redirects")
        (c/map #'extract-relation
          :< "line"
          :fn> ["redirect-from" "redirect-prop" "redirect-to"]
          :> ["redirect-from" "redirect-to"]))

      joined-pipe (->
        [link-pipe redirect-pipe]
        (c/left-join
          [["to"] ["redirect-from"]]
          ["from" "to" "redirect-from" "redirect-to"])
        (c/map #'replace-redirect
          :< ["to" "redirect-to"] :fn> ["redirected"] :> ["redirected"])
        (c/group-by "redirected")
        (c/count "incoming-count")
        (c/group-by "incoming-count" "redirected" true)
        (c/select ["redirected" "incoming-count"])) ;; order by incoming

      ;; map plug in the input files, results serialization and execute
      flow (c/flow
        {"links" (c/lfs-tap (c/text-line "line") link-file)
         "redirects" (c/lfs-tap (c/text-line "line") redirect-file)}
        (c/lfs-tap (c/text-line) out-folder)
        (if to-clj
          (c/map joined-pipe #'serialize-tuple :< Fields/ALL :fn> "line" :> "line")
          joined-pipe))]
      (c/exec flow))))

(defn load-into-model
  "Load RDF files into a Jena model"
  [model files]
  ;; TODO handle compressed files
  (doall
    (map #(do
      (println "loading" (.getAbsolutePath %) "into model...")
      ;; TODO: auto detect format based on file suffix
      (.read model (java.io.FileInputStream. %) nil "N-TRIPLE"))
      files)))

(defn build-model
  "Build model (in memory or in TDB backend) and load RDF files into it"
  ;; TODO: initialise good namespace prefixes
  ([files]
    (build-model nil files))
  ([#^String tdb-directory files]
    (let [model (if (nil? tdb-directory)
      (ModelFactory/createDefaultModel)
      (TDBFactory/createModel tdb-directory))]
      (load-into-model model (map file-str files))
      model)))

(defn index-model
  "Build a lucene index to index all resources of a Model"
  [model fsdirectory]
  (let [analyzer (StandardAnalyzer. Version/LUCENE_30)
        dir (FSDirectory/open (file-str fsdirectory))
        max-len (IndexWriter$MaxFieldLength. 2500)]
    (with-open [index-writer (IndexWriter. dir analyzer true max-len)]
      (println "Implement me!"))))

(defn index-entities
  "Build a lucene index for DBPedia entites"
  [dbpedia-folder fsdirectory]
  (let [files
        (remove #(.isDirectory %)
          (file-seq (file-str dbpedia-folder)))]
    (with-open [model (build-model files)]
      (index-model model fsdirectory))))

