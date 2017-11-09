package phylo.tree.algorithm.flipcut.bcdGraph;

import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Iterator;
import java.util.function.Consumer;

public class BitMapIteratable<T> implements Iterable<T> {
    final T[] source;
    final RoaringBitmap indeces;


    public BitMapIteratable(T[] source, RoaringBitmap indeces) {
        this.source = source;
        this.indeces = indeces;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new BitMapIterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        indeces.forEach((IntConsumer) i -> {
            action.accept(source[i]);
        });
    }

    class BitMapIterator implements Iterator<T> {
        final PeekableIntIterator it = indeces.getIntIterator();

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return source[it.next()];
        }
    }
}
