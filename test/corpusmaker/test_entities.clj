(ns corpusmaker.test-entities
  (:use
    clojure.test
    corpusmaker.entities
    cascading.clojure.io)
  (:require
    [clojure.contrib.java-utils :as ju]
    [clojure.contrib.duck-streams :as ds]
    [cascading.clojure.api :as c])
  (:import cascading.tuple.Fields))

(defn serialize-tuple
  [& tuple-elems]
  (pr-str (vec tuple-elems)))

(def *long-abstract* "test/dbpedia_3.4_longabstract_en.nt")
(def *instance-type* "test/dbpedia_3.4_instancetype_en.nt")

(deftest extract-property-test
  (let [triples (ds/read-lines (ju/file *long-abstract*))
        results (map extract-property triples)]
    (let [[resource property value lang] (first results)]
      (is (= "http://dbpedia.org/resource/!!!" resource))
      (is (= "http://dbpedia.org/property/abstract" property))
      (is (= 321 (.length value)))
      (is (= "!!! is a dance-punk band" (.substring value 0 24)))
      (is (= "en" lang)))))

(deftest extract-relation-test
  (let [triples (ds/read-lines (ju/file *instance-type*))
        results (map extract-relation triples)]
    (let [[source relation target] (first results)]
      (is (= "http://dbpedia.org/resource/!!!" source))
      (is (= "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" relation))
      (is (= "http://www.w3.org/2002/07/owl#Thing" target)))))

(def agg-cons (c/agg cons ()))

(deftest entity-literal-parsing-test
  (with-log-level :warn
    (with-tmp-files [sink (temp-path "corpusmaker-test-sink")]

      (let [p-abstracts ;; first pipe to extract abstracts data
            (->
              (c/pipe "abstracts")
              (c/map #'extract-property
                :< "line"
                :fn> ["resource" "p_abstract" "abstract" "lang"]
                :> ["resource" "abstract"]))
            p-types ;; second pipe to extract typing info
            (->
              (c/pipe "types")
              (c/map #'extract-relation
                :< "line"
                :fn> ["resource" "p_type" "type"]
                :> ["resource" "type"]))
            flow
            (c/flow
              {"abstracts" (c/lfs-tap (c/text-line "line") *long-abstract*)
               "types" (c/lfs-tap (c/text-line "line") *instance-type*)}
              (c/lfs-tap (c/text-line) sink)
              (-> [p-abstracts p-types]
                (c/inner-join
                  [["resource"] ["resource"]]
                  ["resource1" "abstract" "resource2" "type"])
                (c/select ["resource1" "abstract" "type"])
                (c/map #'serialize-tuple :< Fields/ALL :fn> "line" :> "line")))]
        (c/exec flow)
        (let [results (map read-string (ds/read-lines (ju/file sink "part-00000")))]
          (is (= 54 (.size results)))
          (let [[resource value type] (first results)]
            (is (= "http://dbpedia.org/resource/!!!" resource))
            (is (= "!!! is a dance-punk band" (.substring value 0 24)))
            (is (= "http://dbpedia.org/ontology/Band" type))))))))
