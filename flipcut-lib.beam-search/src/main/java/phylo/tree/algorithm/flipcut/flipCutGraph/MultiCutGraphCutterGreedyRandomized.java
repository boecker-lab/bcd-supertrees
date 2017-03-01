package phylo.tree.algorithm.flipcut.flipCutGraph;


import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
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
    public enum SamplingDitribution {UNIFORM,GAUSSIAN}
    private final SamplingDitribution dist = SamplingDitribution.GAUSSIAN;
    private final double maximumBlackListProportion = .5;
    protected Set<FlipCutNodeSimpleWeight> fullBlacklist = new HashSet<>();
    LinkedList<DefaultMultiCut> mincuts = null;

    public MultiCutGraphCutterGreedyRandomized(CutGraphTypes type, FlipCutGraphMultiSimpleWeight graphToCut) {
        super(type, graphToCut);
    }

    @Override
    public DefaultMultiCut getNextCut() {
//        if (DEBUG) System.out.println("Taxa to cut: " + (source.taxa));
        if (mincuts == null) {
            HashSet<DefaultMultiCut> mincuts =  new HashSet<>(source.getK());
             while (mincuts.size() < source.getK() && !stopCutting) {
                DefaultMultiCut r = super.getNextCut();
                if (r != null)
                    mincuts.add(r);
            }
            getRandCuts(mincuts);

            this.mincuts = new LinkedList(mincuts);
            Collections.sort(this.mincuts);
        }


        return mincuts.poll();
    }

    private void getRandCuts(HashSet<DefaultMultiCut> mincuts) {
        fullBlacklist = new HashSet<>(blacklist);
        int it = 0;
//        System.out.println();
//        System.out.println();
        while (mincuts.size() < source.getK() && it < (2*source.getK())) {
//            long time = System.currentTimeMillis();
            it++;
            blacklist = new HashSet<>(drawBlackCharacters(fullBlacklist));
            calculateMinCut();
            if (mincut != null) {
                mincutValue = getMinCutValueAndFillBlacklist(fullBlacklist, mincut);
                mincuts.add(new DefaultMultiCut(mincut, mincutValue, source));
            }
//            System.out.println("On Random Cut needed " + (System.currentTimeMillis() - time) / 1000d + "s");
//            System.out.println();
        }
    }

    private Set<FlipCutNodeSimpleWeight> drawBlackCharacters(Set<FlipCutNodeSimpleWeight> fullBlacklist) {
        List<FlipCutNodeSimpleWeight> bl = new ArrayList<>(fullBlacklist.size());
        long[] weights = new long[fullBlacklist.size()];
        long weight = 0;
        int index =0;
        for (FlipCutNodeSimpleWeight node : fullBlacklist) {
            bl.add(node);
            weight += node.edgeWeight;
            weights[index++] = weight;
        }


        if (bl.size() > 0) {
            Collections.shuffle(bl);
            int upper = Math.min(bl.size() + 1, 2 + (int)Math.round(source.characters.size() * maximumBlackListProportion));


            int number;
            if (dist.equals(SamplingDitribution.UNIFORM)) {
                number = ThreadLocalRandom.current().nextInt(0, upper);
            }else{
                double tmp = ThreadLocalRandom.current().nextGaussian() * .194 + .5; //calculate gaussian between 0 and 1
                tmp = Math.max(0,Math.min(1,tmp));
                number = (int) Math.round(tmp * upper); //round to int value
            }

            Set<FlipCutNodeSimpleWeight> r = new HashSet<>(number);
            for (int i = 0; i<number;i++){
                long search = ThreadLocalRandom.current().nextLong(weight+1);
                int elementIndex = Arrays.binarySearch(weights,search);
                r.add(bl.get(elementIndex<0?(elementIndex*-1)-1:elementIndex));
            }
            return r;
        }
        return new HashSet<>(bl);
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
