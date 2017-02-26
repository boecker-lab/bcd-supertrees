package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 */

import phylo.tree.algorithm.flipcut.mincut.Colorable;
import phylo.tree.algorithm.flipcut.mincut.EdgeColor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Edge implements Colorable {

    private final Set<Vertex> ends = new HashSet<>(2);
    Set<EdgeColor> colors = new HashSet<>();

    public Edge(Vertex fst, Vertex snd) {
        this(fst, snd, 1d);
    }

    public Edge(Vertex fst, Vertex snd, double weight) {
        this(fst, snd, new EdgeColor(weight));
    }

    public Edge(Vertex fst, Vertex snd, EdgeColor color) {
        if (fst == null || snd == null) {
            throw new IllegalArgumentException("Both vertices are required");
        }
        ends.add(fst);
        ends.add(snd);
        if (color != null) {
            add(color);
        }
    }

    public boolean contains(Vertex v1, Vertex v2) {
        return ends.contains(v1) && ends.contains(v2);
    }

    public boolean contains(Vertex v) {
        return ends.contains(v);
    }

    public Iterator<Vertex> iterator() {
        return ends.iterator();
    }

    public Vertex getOppositeVertex(Vertex v) {
        if (!ends.contains(v)) {
            throw new IllegalArgumentException("Vertex " + v.lbl);
        }
        final Iterator<Vertex> it = ends.iterator();
        Vertex v2 = it.next();
        if (v2 != v)
            return v2;
        else
            return it.next();
    }

    public void replaceVertex(Vertex oldV, Vertex newV) {
        if (!ends.contains(oldV)) {
            throw new IllegalArgumentException("Vertex " + oldV.lbl);
        }
        ends.remove(oldV);
        ends.add(newV);
    }

    @Override
    public Iterator<EdgeColor> colorIterator() {
        return new ColorIterator();
    }

    @Override
    public boolean add(EdgeColor color) {
        color.getEdges().add(this);
        return colors.add(color);
    }

    private Edge self(){return this;}

    private class ColorIterator implements Iterator<EdgeColor>{
        private Iterator<EdgeColor> source = colors.iterator();
        private EdgeColor current = null;

        @Override
        public boolean hasNext() {
            return source.hasNext();
        }

        @Override
        public EdgeColor next() {
            current = source.next();
            return current;
        }

        @Override
        public void remove() {
            source.remove();
            current.getEdges().remove(self());
        }
    }
}
