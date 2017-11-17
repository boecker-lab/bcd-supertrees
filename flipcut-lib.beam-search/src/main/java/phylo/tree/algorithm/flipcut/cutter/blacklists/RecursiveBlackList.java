package phylo.tree.algorithm.flipcut.cutter.blacklists;


import org.jetbrains.annotations.NotNull;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.*;

public class RecursiveBlackList extends GreedyBlackList {
    public RecursiveBlackList() {
        super();
    }

    public RecursiveBlackList(@NotNull Collection<? extends FlipCutNodeSimpleWeight> c) {
        super(c);
    }

    public List<? extends BlackList> createBlackLists(final Set<FlipCutNodeSimpleWeight> candidates) {
        if (candidates == null) return Collections.emptyList();
        List<BlackList> r = new ArrayList<>(candidates.size() + 1);
        r.add(new RecursiveBlackList(createGreedyBlackList(candidates)));

        for (FlipCutNodeSimpleWeight candidate : candidates) {
            RecursiveBlackList tmp = new RecursiveBlackList();
            tmp.addAll(this);
            tmp.add(candidate);
            r.add(tmp);
        }
        return r;
    }

    @Override
    public RecursiveBlackList newInitialInstance() {
        return new RecursiveBlackList();
    }
}
