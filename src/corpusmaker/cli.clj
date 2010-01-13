(ns corpusmaker.cli
  (:gen-class)
  (:use corpusmaker.entities
     clojure.contrib.command-line))

(defn handle-wikipedia [args]
  (println "TODO")
  0)

(defn handle-load-types [args]
  (with-command-line
    args
    "WP page URL to DBpedia types database creation using a Redis server"
    [[redis-host "The Redis DB hostname or IP" "localhost"]
     [redis-port "The Redis DB port number" 6379]
     [input-folder i "Folder that holds the DBpedia dumps" "."]
     [flush-db? "Flush the Redis DBs before importing"]
     remaining]
    (println "redis host: " redis-host)
    (println "redis port: " redis-port)
    (println "input folder: " input-folder)
    (println "flush database: " (if flush-db? "yes" "no"))
    (let [server-params {:host redis-host :port redis-port}]
      (try
        (when flush-db?
          (flush-all-dbs server-params))
        ;; TODO: find a way to report progress?
        (time (build-page-type-db input-folder server-params)) 0
        (catch java.net.ConnectException ce
          (println "ERROR: failed to connect to redis host" redis-host
                   "on port" redis-port) 1)
        (catch java.io.FileNotFoundException fnfe
          (println "ERROR: could not load DBpedia dumps:" (.getMessage fnfe))
          2)))))

(def *commands*
  {"load-types" handle-load-types
   "wikipedia" handle-wikipedia})

(defn -main [& args]
  (when args
    (let [command (first args)]
      (let [handler (*commands* command)]
        (if handler
          (System/exit (handler (rest args)))
          (println "ERROR:" command "is not a valid command")))))
  (println "try one of the following commands: " (keys *commands*))
  (System/exit 1))

