package mincut.cutGraphImpl.minCutKargerStein;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 24.02.17.
 */

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class VertexPair {
    public final Vertex v1;
    public final Vertex v2;

    public VertexPair(Vertex v1, Vertex v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VertexPair)) return false;

        VertexPair that = (VertexPair) o;

        return (v1.equals(that.v1) && v2.equals(that.v2) || v1.equals(that.v2) && v2.equals(that.v1));
    }

    @Override
    public int hashCode() {
        return v1.hashCode() + v2.hashCode();
    }
}
