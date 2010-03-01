package corpusmaker.cascading.scheme;

import cascading.scheme.Scheme;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import corpusmaker.hadoop.format.WikipediaPageInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;

import java.io.IOException;

/**
 * Wrap the WikipediaPageInputFormat class as a Cascading source Scheme
 */
public class WikipediaPageScheme extends Scheme {

    public static final Fields SOURCE_FIELDS = new Fields("title", "markup");

    public WikipediaPageScheme() {
        super(SOURCE_FIELDS, Fields.UNKNOWN);
    }

    @Override
    public void sourceInit(Tap tap, JobConf jobConf) throws IOException {
        jobConf.setInputFormat(WikipediaPageInputFormat.class);
    }

    @Override
    public Tuple source(Object title, Object markup) {
        Tuple tuple = new Tuple();
        tuple.add(title.toString());
        tuple.add(markup.toString());
        return tuple;
    }

    @Override
    public void sinkInit(Tap tap, JobConf jobConf) throws IOException {
        throw new UnsupportedOperationException("WikipediaPageScheme cannot be used as Sink");
    }

    @Override
    public void sink(TupleEntry tupleEntry, OutputCollector outputCollector) throws IOException {
        throw new UnsupportedOperationException("WikipediaPageScheme cannot be used as Sink");
    }
}
