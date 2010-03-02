;;   Copyright (c) Olivier Grisel, 2010
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

;; utilities to build cascading flows for Wikipedia content

(ns corpusmaker.cascading.wikipedia
  (:require
    (cascading.clojure [api :as c])
    (corpusmaker [wikipedia :as w]))
  (:import
    corpusmaker.cascading.scheme.WikipediaPageScheme
    info.bliki.wiki.model.WikiModel
    corpusmaker.wikipedia.LinkAnnotationTextConverter
    corpusmaker.wikipedia.Annotation))

(defn wikipedia-page-scheme
  "Build WikipediaPageScheme with article title and markup fields"
  {:fields ["title" "markup"]}
  []
  (WikipediaPageScheme.))

(defn wikipedia-tap
  "Build a source tap from a wikipedia XML dump"
  [input-path-or-file]
  (c/hfs-tap (wikipedia-page-scheme) input-path-or-file))

(defn remove-redirect
  "Filter pages that carry a #REDIRECT marker"
  [#^Pipe previous]
  (c/filter previous ["markup"] #'w/no-redirect?))

(defn unigrams
  "Compute the tokens vector of the markup content"
  [#^Pipe previous]
  (c/mapcat previous ["markup"] ["unigram" #'w/tokenize] ["title" "unigram"]))
