/* Copyright (c) Olivier Grisel, 2009
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */
package corpusmaker;

import info.bliki.htmlcleaner.ContentToken;
import info.bliki.htmlcleaner.TagNode;
import info.bliki.wiki.filter.ITextConverter;
import info.bliki.wiki.filter.WPList;
import info.bliki.wiki.filter.WPTable;
import info.bliki.wiki.model.Configuration;
import info.bliki.wiki.model.IWikiModel;
import info.bliki.wiki.model.ImageFormat;
import info.bliki.wiki.tags.WPATag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CorpusMakerTextConverter implements ITextConverter {

    public static final String HREF_ATTR_KEY = "href";

    public static final String WIKILINK_ATTR_KEY = "wikilink";

    public static final String WIKIOBJECT_ATTR_KEY = "wikiobject";

    public static final Pattern INTERWIKI_PATTERN = Pattern.compile("http://[\\w-]+\\.wikipedia\\.org/wiki/.*");

    protected static final List<Annotation> wikilinks = new ArrayList<Annotation>();

    @SuppressWarnings("unchecked")
    @Override
    public void nodesToText(List<? extends Object> nodes, Appendable buffer,
            IWikiModel model) throws IOException {
        CountingAppendable countingBuffer;
        if (buffer instanceof CountingAppendable) {
            countingBuffer = (CountingAppendable) buffer;
        } else {
            // wrap
            countingBuffer = new CountingAppendable(buffer);
        }

        if (nodes != null && !nodes.isEmpty()) {
            try {
                int level = model.incrementRecursionLevel();
                if (level > Configuration.RENDERER_RECURSION_LIMIT) {
                    countingBuffer.append("Error - recursion limit exceeded rendering tags in PlainTextConverter#nodesToText().");
                    return;
                }
                for (Object node : nodes) {
                    if (node instanceof WPATag) {
                        // extract wikilink annotations
                        WPATag tag = (WPATag) node;
                        String wikilink = (String) tag.getObjectAttributes().get(
                                WIKILINK_ATTR_KEY);
                        if (wikilink != null) {
                            int colonIdx = wikilink.indexOf(':');
                            if (colonIdx == -1) {
                                // do not serialize non-topic wiki-links such as
                                // translation links missing from the
                                // INTERWIKI_LINK map
                                int start = countingBuffer.currentPosition;
                                tag.getBodyString(countingBuffer);
                                int end = countingBuffer.currentPosition;
                                wikilinks.add(new Annotation(start, end,
                                        wikilink));
                            }
                        } else {
                            tag.getBodyString(countingBuffer);
                        }
                    } else if (node instanceof List) {
                        nodesToText((List) node, countingBuffer, model);
                    } else if (node instanceof ContentToken) {
                        ContentToken contentToken = (ContentToken) node;
                        countingBuffer.append(contentToken.getContent());
                    } else if (node instanceof WPList) {
                        ((WPList) node).renderPlainText(this, countingBuffer,
                                model);
                    } else if (node instanceof WPTable) {
                        ((WPTable) node).renderPlainText(this, countingBuffer,
                                model);
                    } else if (node instanceof TagNode) {
                        TagNode tagNode = (TagNode) node;
                        Map<String, String> attributes = tagNode.getAttributes();
                        Map<String, Object> oAttributes = tagNode.getObjectAttributes();
                        boolean hasSpecialHandling = false;
                        if (tagNode.getName().equals("a")) {
                            String href = attributes.get(HREF_ATTR_KEY);
                            if (INTERWIKI_PATTERN.matcher(href).matches()) {
                                hasSpecialHandling = true;
                                // ignore the link since this most probably for
                                // translation purpose only.
                            }
                        } else if (oAttributes != null
                                && oAttributes.get(WIKIOBJECT_ATTR_KEY) instanceof ImageFormat) {
                            hasSpecialHandling = true;
                            ImageFormat iformat = (ImageFormat) oAttributes.get(WIKIOBJECT_ATTR_KEY);
                            imageNodeToText(tagNode, iformat, countingBuffer,
                                    model);
                        }
                        if (!hasSpecialHandling) {
                            nodesToText(tagNode.getChildren(), countingBuffer,
                                    model);
                        }
                        if (tagNode.getName().equals("p")
                                || tagNode.getName().equals("div")) {
                            countingBuffer.append("\n");
                        }
                    }
                }
            } finally {
                model.decrementRecursionLevel();
            }
        }
    }

    @Override
    public void imageNodeToText(TagNode tagNode, ImageFormat imageFormat,
            Appendable buffer, IWikiModel model) throws IOException {
        nodesToText(tagNode.getChildren(), buffer, model);
    }

    @Override
    public boolean noLinks() {
        return true;
    }

    public List<Annotation> getWikiLinks() {
        return wikilinks;
    }

    public class Annotation {

        public final int start;

        public final int end;

        public final String label;

        public Annotation(int start, int end, String label) {
            this.start = start;
            this.end = end;
            this.label = label;
        }

    }

    public class CountingAppendable implements Appendable {

        public int currentPosition = 0;

        final protected Appendable wrappedBuffer;

        public CountingAppendable(Appendable wrappedBuffer) {
            this.wrappedBuffer = wrappedBuffer;
        }

        @Override
        public Appendable append(CharSequence charSeq) throws IOException {
            currentPosition += charSeq.length();
            return wrappedBuffer.append(charSeq);
        }

        @Override
        public Appendable append(char aChar) throws IOException {
            currentPosition += 1;
            return wrappedBuffer.append(aChar);
        }

        @Override
        public Appendable append(CharSequence charSeq, int start, int end)
                throws IOException {
            currentPosition += end - start;
            return wrappedBuffer.append(charSeq, start, end);
        }

    }

}
