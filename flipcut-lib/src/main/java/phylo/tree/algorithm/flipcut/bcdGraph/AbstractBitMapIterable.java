package phylo.tree.algorithm.flipcut.bcdGraph;

import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.util.Iterator;
import java.util.function.Consumer;

public abstract class AbstractBitMapIterable<T> implements Iterable<T> {
    final RoaringBitmap indeces;


    protected AbstractBitMapIterable(RoaringBitmap indeces) {
        this.indeces = indeces;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new BitMapIterator();
    }

    protected abstract T getFromSource(int index);

    @Override
    public void forEach(Consumer<? super T> action) {
        indeces.forEach((IntConsumer) i -> {
            action.accept(getFromSource(i));
        });
    }

    protected class BitMapIterator implements Iterator<T> {
        final PeekableIntIterator it = indeces.getIntIterator();

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return getFromSource(it.next());
        }
    }
}
