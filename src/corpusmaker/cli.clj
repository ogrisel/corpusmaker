(ns corpusmaker.cli
  (:gen-class)
  (:use corpusmaker.wikipedia
    corpusmaker.entities
    clojure.contrib.command-line))

(defn handle-count-incoming [args]
  (with-command-line
    args
    "Compute the incoming links count for dbpedia resources"
    [[pagelinks-file "The input file in N-TRIPLE format"]
     [redirect-file "The redirect file in N-TRIPLE format"]
     [output-folder "The output folder" "."] ;; TODO handle HDFS URLs
     remaining]
    (try
      ;; TODO: find a way to report progress?
      (time (count-incoming pagelinks-file output-folder)) 0
      (catch java.io.FileNotFoundException fnfe
        (println "ERROR:" (.getMessage fnfe)) 2))))

(defn handle-chunk [args]
  (with-command-line
    args
    "Chunk a Wikipedia pages XML dump into smaller files"
    [[input-file "The input file"]
     [sdtin? "Stream the complete uncompressed dump to STDIN" false]
     [output-folder "The output folder" "."] ;; TODO handle HDFS URLs
     [chunk-size "The maximum size of a chunk in megabytes" 128]
     remaining]
    (try
      ;; TODO: find a way to report progress?
      (time (chunk-dump input-file output-folder chunk-size)) 0
      (catch java.io.FileNotFoundException fnfe
        (println "ERROR:" (.getMessage fnfe)) 2))))

(defn handle-build-index [args]
  (with-command-line
    args
    "Load DBpedia resources into a lucene fulltext index"
    [[input-folder i "Folder that holds the DBpedia dumps" "."]
     [index-dir d "Lucene FSDirectory location" "."]
     remaining]
    (println "indexing dbpedia content of" input-folder "into" index-dir)
    (try
      ;; TODO: find a way to report progress?
      (time (index-entities input-folder index-dir)) 0
      (catch java.io.FileNotFoundException fnfe
        (println "ERROR: could not load DBpedia dumps:" (.getMessage fnfe))
        2)
      (catch java.io.IOError ioe
        (println "ERROR: failed to create lucene index:" (.getMessage ioe))
        3))))

(def *commands*
  {"build-index" handle-build-index
   "count-incoming" handle-count-incoming
   "chunk" handle-chunk})

(defn -main [& args]
  (when args
    (let [command (first args)]
      (let [handler (*commands* command)]
        (if handler
          (System/exit (handler (rest args)))
          (println "ERROR:" command "is not a valid command")))))
  (println "try one of the following commands: " (keys *commands*))
  (System/exit 1))

