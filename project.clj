(defproject corpusmaker "0.1.0-SNAPSHOT"
  :description "Utilities to build NLP corpus from Wikipedia / DBPedia dumps"
  :url "http://github.com/ogrisel/corpusmaker"
  :repositories {"info-bliki" "http://gwtwiki.googlecode.com/svn/maven-repository/"}
  :dependencies [[org.clojure/clojure "1.1.0-master-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.1.0-master-SNAPSHOT"]
                 [redis-clojure/redis-clojure "1.0-SNAPSHOT"]
                 [stax/stax "1.2.0"]
                 [info.bliki.wiki/bliki-core "3.0.13"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]
                     [lein-javac "0.0.2-SNAPSHOT"]
                     [org.clojure/swank-clojure "1.0"]])
