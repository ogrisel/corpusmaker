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

## Usage

TODO: commandline interface to build the links to type redis DB and launch
local or distribute MapReduce jobs for corpus extraction.

