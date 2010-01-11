(defproject corpusmaker "1.0.0-SNAPSHOT"
  :description "Utilities to build NLP corpus from Wikipedia / DBPedia dumps"
  :url "http://github.com/ogrisel/corpusmaker"
  :dependencies [[org.clojure/clojure "1.1.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.1.0-master-SNAPSHOT"]
                 [redis-clojure/redis-clojure "1.0-SNAPSHOT"]
                 [info.bliki.wiki/bliki-core "3.0.14-SNAPSHOT"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]
                     [lein-javac "0.0.2-SNAPSHOT"]
                     [org.clojure/swank-clojure "1.0"]])
