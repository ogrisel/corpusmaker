(defproject corpusmaker "0.1.0-SNAPSHOT"
  :description "Utilities to build NLP corpus from Wikipedia / DBPedia dumps"
  :url "http://github.com/ogrisel/corpusmaker"
  :repositories {"info-bliki" "http://gwtwiki.googlecode.com/svn/maven-repository/"}
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [redis-clojure "1.0-SNAPSHOT"]
                 [stax "1.2.0"]
                 [info.bliki.wiki/bliki-core "3.0.13"]
                 [commons-lang "2.4"]
                 [org.apache.lucene/lucene-core "3.0.1"]
                 [org.apache.lucene/lucene-wikipedia "3.0.1"]
                 [cascading-clojure "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]
                     [lein-javac "0.0.2-SNAPSHOT"]])
