package phylo.tree.algorithm.flipcut.cutter.blacklists;


import org.jetbrains.annotations.NotNull;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.*;

public class GreedyBlackList extends BlackList {
    public GreedyBlackList() {
        super();
    }

    public GreedyBlackList(@NotNull Collection<? extends FlipCutNodeSimpleWeight> c) {
        super(c);
    }

    public List<? extends BlackList> createBlackLists(final Set<FlipCutNodeSimpleWeight> candidates) {
        if (candidates == null) return Collections.emptyList();
        List<BlackList> r = new ArrayList<>(candidates.size() + 1);
        r.add(createGreedyBlackList(candidates));
        return r;
    }

    protected BlackList createGreedyBlackList(final Set<FlipCutNodeSimpleWeight> candidates) {
        GreedyBlackList tmp = new GreedyBlackList(this);
        tmp.addAll(candidates);
        return tmp;
    }

    @Override
    public GreedyBlackList newInitialInstance() {
        return new GreedyBlackList();
    }
}
