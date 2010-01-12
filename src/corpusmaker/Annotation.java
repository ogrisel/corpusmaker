package corpusmaker;

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
