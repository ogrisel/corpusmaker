package corpusmaker.hadoop.format;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Scan a media wiki markup dump to read page title as key and page markup payload as value.
 */
public class WikipediaPageInputFormat extends FileInputFormat<Text, Text> {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public RecordReader<Text, Text> getRecordReader(InputSplit inputSplit,
                                                    JobConf jobConf, Reporter reporter) throws IOException {
        return new WikipediaRecordReader((FileSplit) inputSplit, jobConf);
    }

    /**
     * WikipediaRecordReader class to read through a given xml to output the page article
     * title and un-escaped markup payload.
     */
    public static class WikipediaRecordReader implements
            RecordReader<Text, Text> {
        public static final byte[] START_TITLE_MARKER = "<title>".getBytes(UTF8);
        public static final byte[] END_TITLE_MARKER = "</title>".getBytes(UTF8);
        public static final byte[] START_TEXT_MARKER = "<text xml:space=\"preserve\">".getBytes(UTF8);
        public static final byte[] END_TEXT_MARKER = "</text>".getBytes(UTF8);
        private final long start;
        private final long end;
        private final FSDataInputStream fsin;
        private final DataOutputBuffer buffer = new DataOutputBuffer();

        public WikipediaRecordReader(FileSplit split, JobConf jobConf) throws IOException {
            // open the file and seek to the start of the split
            start = split.getStart();
            end = start + split.getLength();
            Path file = split.getPath();
            FileSystem fs = file.getFileSystem(jobConf);
            fsin = fs.open(split.getPath());
            fsin.seek(start);
        }

        public boolean next(Text key, Text value) throws IOException {
            if (fsin.getPos() < end) {
                try {
                    if (readUntilMatch(START_TITLE_MARKER, false)) {
                        if (readUntilMatch(END_TITLE_MARKER, true)) {
                            int stop =  buffer.getLength() - END_TITLE_MARKER.length;
                            key.set(buffer.getData(), 0, stop);
                            buffer.reset();
                            if (readUntilMatch(START_TEXT_MARKER, false)) {
                                if (readUntilMatch(END_TEXT_MARKER, true)) {
                                    // un-escape the XML entities encoding and re-encode the result as raw UTF8 bytes
                                    stop =  buffer.getLength() - END_TITLE_MARKER.length;
                                    String xmlEscapedContent = new String(buffer.getData(), 0, stop, UTF8);
                                    value.set(StringEscapeUtils.unescapeXml(xmlEscapedContent).getBytes(UTF8));
                                    return true;
                                }
                            }
                        }
                    }
                } finally {
                    buffer.reset();
                }
            }
            return false;
        }

        public Text createKey() {
            return new Text();
        }

        public Text createValue() {
            return new Text();
        }

        public long getPos() throws IOException {
            return fsin.getPos();
        }

        public void close() throws IOException {
            fsin.close();
        }

        public float getProgress() throws IOException {
            return (fsin.getPos() - start) / (float) (end - start);
        }

        private boolean readUntilMatch(byte[] match, boolean withinBlock)
                throws IOException {
            int i = 0;
            while (true) {
                int b = fsin.read();
                // end of file:
                if (b == -1) return false;
                // save to buffer:
                if (withinBlock) buffer.write(b);

                // check if we're matching:
                if (b == match[i]) {
                    i++;
                    if (i >= match.length) return true;
                } else i = 0;
                // see if we've passed the stop point:
                if (!withinBlock && i == 0 && fsin.getPos() >= end) return false;
            }
        }
    }

}
