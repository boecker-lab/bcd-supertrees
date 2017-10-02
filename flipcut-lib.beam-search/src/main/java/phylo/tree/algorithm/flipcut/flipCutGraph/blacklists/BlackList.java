package phylo.tree.algorithm.flipcut.flipCutGraph.blacklists;

import org.jetbrains.annotations.NotNull;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BlackList extends HashSet<FlipCutNodeSimpleWeight> {
    protected int numberOfCharacters =  Integer.MAX_VALUE;

    public BlackList() {
    }

    public BlackList(@NotNull Collection<? extends FlipCutNodeSimpleWeight> c) {
        super(c);
    }

    public abstract List<? extends BlackList> createBlackLists(final Set<FlipCutNodeSimpleWeight> candidates);
    public abstract BlackList newInitialInstance();

    public void setNumberOfCharacters(int numberOfCharacters) {
        this.numberOfCharacters = numberOfCharacters;
    }
}
