package phylo.tree.algorithm.flipcut.flipCutGraph;


import phylo.tree.algorithm.flipcut.model.DefaultMultiCut;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import static phylo.tree.algorithm.flipcut.costComputer.CostComputer.ACCURACY;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 *         Date: 19.04.13
 *         Time: 12:02
 */
public class MultiCutGraphCutterGreedyRandomized extends MultiCutGraphCutterGreedy implements MultiCutter<FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
    protected Set<FlipCutNodeSimpleWeight> fullBlacklist = new HashSet<>();
    ArrayDeque<DefaultMultiCut> mincuts = null;

    public MultiCutGraphCutterGreedyRandomized(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut) {
        super(type, graphToCut);
    }

    @Override
    public DefaultMultiCut getNextCut() {
//        if (DEBUG) System.out.println("Taxa to cut: " + (source.taxa));
        if (mincuts == null) {
            mincuts = new ArrayDeque<>(source.maxCutNumber);
           //todo reenable
             while (mincuts.size() < source.maxCutNumber && !stopCutting) {
                DefaultMultiCut r = super.getNextCut();
                if (r != null)
                    mincuts.add(r);
            }
            getRandCuts();
        }


        return mincuts.pollFirst();
    }

    private void getRandCuts() {
        fullBlacklist = new HashSet<>(blacklist);
        int it = 0;
        while (mincuts.size() < source.maxCutNumber && it < (2*source.maxCutNumber)) {
            it++;
            blacklist = new HashSet<>(drawBlackCharacters(fullBlacklist));
            calculateMinCut();
            if (mincut != null) {
                mincutValue = getMinCutValueAndFillBlacklist(fullBlacklist, mincut);
                mincuts.add(new DefaultMultiCut(mincut, mincutValue, source));
                System.out.println("###### MinCutValue RAND ########");
                System.out.println(mincutValue);
                System.out.println(mincutValue2);
                System.out.println(CutGraphCutter.INFINITY * ACCURACY);
                System.out.println(Long.MAX_VALUE);
                System.out.println("###########################");
            }
        }
    }

    private List<FlipCutNodeSimpleWeight> drawBlackCharacters(Set<FlipCutNodeSimpleWeight> fullBlacklist) {
        //todo debug
        if (!source.characters.containsAll(fullBlacklist)){
            System.out.println("proof");
        }
        List<FlipCutNodeSimpleWeight> bl = new ArrayList<>(fullBlacklist);
        if (bl.size() > 0) {
            Collections.shuffle(bl);
            int upper = Math.min(bl.size() + 1, 2 + source.characters.size()/3);
            int randUpper = ThreadLocalRandom.current().nextInt(1, upper);
            bl = bl.subList(0,randUpper);
        }
        return bl;
    }

    static class Factory implements MultiCutterFactory<MultiCutGraphCutterGreedyRandomized, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight>, MaxFlowCutterFactory<MultiCutGraphCutterGreedyRandomized, FlipCutNodeSimpleWeight, FlipCutGraphMultiSimpleWeight> {
        private final CutGraphTypes type;

        Factory(CutGraphTypes type) {
            this.type = type;
        }

        @Override
        public MultiCutGraphCutterGreedyRandomized newInstance(FlipCutGraphMultiSimpleWeight graph) {
            return new MultiCutGraphCutterGreedyRandomized(type, graph);
        }

        @Override
        public MultiCutGraphCutterGreedyRandomized newInstance(FlipCutGraphMultiSimpleWeight graph, ExecutorService executorService, int threads) {
            return newInstance(graph);
        }

        @Override
        public CutGraphTypes getType() {
            return type;
        }
    }
}
