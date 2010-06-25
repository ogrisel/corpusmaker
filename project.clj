(defproject corpusmaker "0.1.0-SNAPSHOT"
  :description "Utilities to build NLP corpus from Wikipedia / DBPedia dumps"
  :url "http://github.com/ogrisel/corpusmaker"
  :repositories {"info-bliki" "http://gwtwiki.googlecode.com/svn/maven-repository/"}
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [stax "1.2.0"]
                 [info.bliki.wiki/bliki-core "3.0.13"]
                 [commons-lang "2.4"]
                 [org.apache.lucene/lucene-core "3.0.1"]
                 [org.apache.lucene/lucene-wikipedia "3.0.1"]
                 [com.hp.hpl.jena/jena "2.6.2"]
                 [com.hp.hpl.jena/tdb "0.8.4"]
                 [com.hp.hpl.jena/arq "2.8.2"]
                 [log4j "1.2.13" 
                   :exclusions [javax.mail/mail
                                javax.jms/jms
                                com.sun.jdmk/jmxtools
                                com.sun.jmx/jmxri]]
                 [cascading-clojure "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[lein-clojars "0.5.0-SNAPSHOT"]
                     [lein-javac "0.0.2-SNAPSHOT"]]
  :main corpusmaker.cli
  :source-path "src"
  :library-path "lib"
  :test-path "test"
  :jar-dir "target/" ; where to place the project's jar file
  :jvm-opts "-Xmx1g")
