(ns corpusmaker.test-entities
  (:use
    clojure.test
    corpusmaker.entities
    cascading.clojure.io)
  (:require
    [clojure.contrib.java-utils :as ju]
    [clojure.contrib.duck-streams :as ds]
    [cascading.clojure.api :as c]))

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

(deftest entity-literal-parsing-test
  (with-log-level :warn
    (with-tmp-files [sink (temp-path "corpusmaker-test-sink")]
      (let [flow (c/flow
        {"abstracts" (c/lfs-tap (c/text-line "line") *long-abstract*)}
        (c/lfs-tap (c/clojure-line) sink)
        (->
          (c/pipe "abstracts")
          (c/map #'extract-property :< "line" :> ["resource" "value"])))]
        (c/exec flow)
        (let [results (map read-string (ds/read-lines (ju/file sink "part-00000")))]
          (is (= 23 (.size results))))))))

