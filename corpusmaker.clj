;   Copyright (c) Olivier Grisel, 2009
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

; corpusmaker - Clojure tools to build training dataset for machine learning
; based NLP algorithms out of Wikimedia dumps

(ns corpusmaker
    (:require [clojure.zip :as zip]
              [clojure.contrib.lazy-xml :as lxml]
              [clojure.contrib.zip-filter.xml :as zfx]))

; TODO: rewrite this using http://github.com/marktriggs/xml-picker-seq since
; using a zipper keeps all the parsed elements in memory which is not suitable
; for large XML chunks

(defn parse-xml
  "Zipable XML content from any common source"
  [src] (zip/xml-zip (lxml/parse-trim src)))

(defn collect-text
  "collect wikimarkup payload of a dump in seqable xml"
  [xml] (zfx/xml-> xml :page :revision :text zfx/text))

; sample timed run
; (time (dorun (collect-text (parse-xml "chunk-0001.xml"))))
