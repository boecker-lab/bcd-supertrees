package phylo.tree.algorithm.flipcut.cutter.blacklists;

import org.jetbrains.annotations.NotNull;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RandomizedBlackList extends GreedyBlackList {
    public enum SamplingDitribution {UNIFORM, GAUSSIAN}

    private final static SamplingDitribution dist = SamplingDitribution.GAUSSIAN;
    private final static double maximumBlackListProportion = .75;
    private boolean greedyMode = true;

    private RandomizedBlackList(@NotNull Collection<? extends FlipCutNodeSimpleWeight> c, int numberOfCharacters) {
        this(c);
        this.numberOfCharacters = numberOfCharacters;
    }

    public RandomizedBlackList(@NotNull Collection<? extends FlipCutNodeSimpleWeight> c) {
        super(c);
    }

    public RandomizedBlackList() {
        super();
    }

    public List<? extends BlackList> createBlackLists(final Set<FlipCutNodeSimpleWeight> candidates) {
        List<BlackList> r;
        if (candidates != null && greedyMode) {
            r = new ArrayList<>(candidates.size() + 1);
            r.add(new RandomizedBlackList(createGreedyBlackList(candidates), numberOfCharacters));
        } else {
            r = new ArrayList<>(1);
            RandomizedBlackList bl = new RandomizedBlackList(drawBlackCharacters(), numberOfCharacters);
            bl.greedyMode = false;
            r.add(bl);
        }
        return r;
    }

    private Set<FlipCutNodeSimpleWeight> drawBlackCharacters() {
        List<FlipCutNodeSimpleWeight> bl = new ArrayList<>(size());
        long[] weights = new long[size()];
        long weight = 0;
        int index = 0;
        for (FlipCutNodeSimpleWeight node : this) {
            bl.add(node);
            weight += node.getEdgeWeight();
            weights[index++] = weight;
        }

        if (bl.size() > 0) {
            Collections.shuffle(bl);
            int upper = Math.min(bl.size() + 1, 2 + (int) Math.round(numberOfCharacters * maximumBlackListProportion));


            int number;
            if (dist.equals(SamplingDitribution.UNIFORM)) {
                number = ThreadLocalRandom.current().nextInt(0, upper);
            } else {
                double tmp = ThreadLocalRandom.current().nextGaussian() * .194 + .5; //calculate gaussian between 0 and 1
                tmp = Math.max(0, Math.min(1, tmp));
                number = (int) Math.round(tmp * upper); //round to int value
            }

            Set<FlipCutNodeSimpleWeight> r = new HashSet<>(number);
            for (int i = 0; i < number; i++) {
                long search = ThreadLocalRandom.current().nextLong(weight + 1);
                int elementIndex = Arrays.binarySearch(weights, search);
                r.add(bl.get(elementIndex < 0 ? (elementIndex * -1) - 1 : elementIndex));
            }
            return r;
        }
        return new HashSet<>(bl);
    }

    @Override
    public RandomizedBlackList newInitialInstance() {
        return new RandomizedBlackList();
    }
}
