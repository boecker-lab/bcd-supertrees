package phylo.tree.algorithm.flipcut.flipCutGraph;

import java.util.Iterator;

public class NodeLabelIterator<N extends AbstractFlipCutNode<N>> implements Iterator<String> {
    final Iterator<N> taxaIterator;

    public NodeLabelIterator(Iterable<N> taxa) {
        this.taxaIterator = taxa.iterator();
    }

    @Override
    public boolean hasNext() {
        return taxaIterator.hasNext();
    }

    @Override
    public String next() {
        return taxaIterator.next().name;
    }
}