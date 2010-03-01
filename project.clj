(defproject corpusmaker "0.1.0-SNAPSHOT"
  :description "Utilities to build NLP corpus from Wikipedia / DBPedia dumps"
  :url "http://github.com/ogrisel/corpusmaker"
  :repositories {"info-bliki" "http://gwtwiki.googlecode.com/svn/maven-repository/"}
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [redis-clojure "1.0-SNAPSHOT"]
                 [stax "1.2.0"]
                 [info.bliki.wiki/bliki-core "3.0.13"]
                 [org.apache.lucene/lucene-core "3.0.1"]
                 [org.apache.lucene/lucene-wikipedia "3.0.1"]
                 [cascading/cascading "1.0.17-SNAPSHOT"
                   :exclusions [javax.mail/mail janino/janino]]
                 [cascading-clojure "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]
                     [lein-javac "0.0.2-SNAPSHOT"]
                     [org.clojure/swank-clojure "1.0"]])
