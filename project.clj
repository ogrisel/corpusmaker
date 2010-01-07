(defproject corpusmaker "1.0.0-SNAPSHOT"
  :description "Utilities to build NLP corpus from Wikipedia / DBPedia dumps"
  :url "http://github.com/ogrisel/corpusmaker"
  :repositories {"semweb4j" "http://semweb4j.org/repo/"}
  :dependencies [[org.clojure/clojure "1.1.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.1.0-master-SNAPSHOT"]
                 [redis-clojure/redis-clojure "1.0-SNAPSHOT"]
                 [org.wikimodel/wem "2.0.6"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]
                     [org.clojure/swank-clojure "1.0"]])
