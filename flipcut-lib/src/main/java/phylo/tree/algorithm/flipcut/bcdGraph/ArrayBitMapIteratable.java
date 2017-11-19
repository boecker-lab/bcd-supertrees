package phylo.tree.algorithm.flipcut.bcdGraph;

import org.roaringbitmap.RoaringBitmap;

public class ArrayBitMapIteratable<T> extends AbstractBitMapIterable<T> {
    private final T[] source;

    public ArrayBitMapIteratable(T[] source, RoaringBitmap indeces) {
        super(indeces);
        this.source = source;
    }

    @Override
    protected T getFromSource(int index) {
        return source[index];
    }
}
