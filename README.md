# corpusmaker

Clojure utilities to build training corpora for machine learning / NLP out of
public Wikipedia / DBPedia dumps.

## Build from source

Get the latest version of leiningen to build from the sources:

1. [Download the script](http://github.com/technomancy/leiningen/raw/stable/bin/lein).
2. Place it on your path and chmod it to be executable.
3. Run: <tt>lein self-install</tt>

Then, at the root of the corpusmaker source tree:

    $ lein deps # install dependencies in lib/

    $ lein compile-java # compile a custom helper class for the Wikipedia parser

    $ lein compile # ahead-of-time compile clojure tools into classes/

    $ lein test [TESTS] # run the tests in the TESTS namespaces, or all tests

    $ lein repl # launch a REPL with the project classpath configured

    $ lein clean # remove all build artifacts

## Fetching the data

You can get the latest wikipedia dumps for the english articles here (around
5.4GB compressed, 23 GB uncompressed):

  http://download.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2

The DBPedia links and entities types datasets are available here:

  http://downloads.dbpedia.org/3.4/en/wikipage_en.nt.bz2

  http://downloads.dbpedia.org/3.4/en/instancetype_en.nt.bz2

All of those datasets are also available from the Amazon cloud as public EBS
volumes:

  http://developer.amazonwebservices.com/connect/entry.jspa?externalID=2506

  http://developer.amazonwebservices.com/connect/entry.jspa?externalID=231

It is planned to have crane based utility function to load them to HDFS
directly from the EBS volume.

## Usage

TODO: commandline interface to build the links to type redis DB and launch
local or distribute MapReduce jobs for corpus extraction.

