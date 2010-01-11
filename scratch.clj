(use :reload 'corpusmaker.wikipedia)
(import 'corpusmaker.CorpusMakerTextConverter
        'info.bliki.wiki.filter.PlainTextConverter
        'info.bliki.wiki.model.WikiModel
        'java.io.StringReader)

(def *sample-dumpfile* "test/enwiki-20090902-pages-articles-sample.xml")
(def articles (-> *sample-dumpfile* parse-xml collect-text))
(def anarchism (first articles))

(defn parse-markup
  [page-markup]
  (let [model (WikiModel.
                "http:/en.wikipedia.org/wiki/${image}"
                "http://en.wikipedia.org/wiki/${title}")
        converter (CorpusMakerTextConverter.)
        text (.render model converter page-markup)]
    {:text text :categories (-> model (.getCategories) (.keySet) set)
     :links (vec (map #(hash-map
                         :label (.label %) :start (.start %) :end (.end %))
                      (.getWikiLinks converter)))}))

(def simple-markup "[[outlink]] image: [[Image:fichier.jpeg|thumb|[[Category:Cat1]] [[link here]] [[link| there there]] ''italics here'' something ''more italics'' [[Category:Cat2]]]]")

(parse-markup simple-markup)

