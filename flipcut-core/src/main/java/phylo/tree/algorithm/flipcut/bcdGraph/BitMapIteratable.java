package phylo.tree.algorithm.flipcut.bcdGraph;

import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Iterator;
import java.util.function.Consumer;

public class BitMapIteratable implements Iterable<RoaringBitmap> {
    final RoaringBitmap[] source;
    final RoaringBitmap indeces;


    public BitMapIteratable(RoaringBitmap[] source, RoaringBitmap indeces) {
        this.source = source;
        this.indeces = indeces;
    }

    @NotNull
    @Override
    public Iterator<RoaringBitmap> iterator() {
        return new BitMapIterator();
    }

    @Override
    public void forEach(Consumer<? super RoaringBitmap> action) {
        indeces.forEach((IntConsumer) i -> {
            action.accept(source[i]);
        });
    }

    class BitMapIterator implements Iterator<RoaringBitmap> {
        final PeekableIntIterator it = indeces.getIntIterator();

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public RoaringBitmap next() {
            return source[it.next()];
        }
    }
}
