package phylo.tree.algorithm.flipcut;

import mincut.cutGraphAPI.bipartition.MultiCut;

import java.util.Iterator;

public interface SourceTreeGraphMultiCut<C,G extends SourceTreeGraphMultiCut<C,G>> extends SourceTreeGraph<C> {
    int getK();

    boolean containsCuts();

    Iterator<MultiCut<C,G>> getCutIterator();

    void close();
}
