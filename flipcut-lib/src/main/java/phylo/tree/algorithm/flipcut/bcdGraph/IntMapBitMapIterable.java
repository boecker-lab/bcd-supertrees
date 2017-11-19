package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.map.TIntObjectMap;
import org.roaringbitmap.RoaringBitmap;

public class IntMapBitMapIterable<T> extends AbstractBitMapIterable<T> {
    private final TIntObjectMap<T> source;

    public IntMapBitMapIterable(TIntObjectMap<T> source, RoaringBitmap indeces) {
        super(indeces);
        this.source = source;
    }

    @Override
    protected T getFromSource(int index) {
        return source.get(index);
    }
}
