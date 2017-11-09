package phylo.tree.algorithm.flipcut;

import core.algorithm.Algorithm;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.costComputer.FlipCutWeights;
import phylo.tree.algorithm.flipcut.costComputer.UnitCostComputer;
import phylo.tree.algorithm.flipcut.costComputer.WeightCostComputer;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import phylo.tree.algorithm.flipcut.flipCutGraph.MultiCutter;
import phylo.tree.algorithm.flipcut.flipCutGraph.MultiCutterFactory;
import phylo.tree.algorithm.flipcut.model.Partition;
import phylo.tree.model.Tree;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 13:56
 */
public class FlipCutMultiCut extends AbstractFlipCut<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight, MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, MultiCutterFactory<MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>> {
    protected int numberOfCuts = 1;
    protected List<Tree> result;

    public FlipCutMultiCut() {
        super();
    }

    @Override
    protected String name() {
        return getClass().getSimpleName();
    }

    public FlipCutMultiCut(MultiCutterFactory<MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> type) {
        super(type);
    }

    public FlipCutMultiCut(Logger log, MultiCutterFactory<MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> type) {
        super(log, type);
    }

    public FlipCutMultiCut(Logger log, ExecutorService executorService1, MultiCutterFactory<MultiCutter<LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight>, LinkedHashSet<FlipCutNodeSimpleWeight>, FlipCutGraphMultiSimpleWeight> type) {
        super(log, executorService1, type);
    }

    private void calculateSTs() {
        //init fields
        result = null;


        if (initialGraph != null) {
            long calctime = System.currentTimeMillis();
            //map to store partitions which are already cutted in more parts than the others
            final TreeMap<Integer, Set<Partition>> subsBench = new TreeMap<>();
            final int numTaxa = initialGraph.taxa.size();

            System.out.println("Calculating Partitions...");

            //initial step to generate
            final LinkedList<Partition> partitions = new Partition(0, initialGraph).getKBestNew(numberOfCuts, Long.MAX_VALUE);
            int minimalPartLevel = buildNextPartitionLevel(partitions, subsBench);
            initialGraph = null; //get rid of these large graph

            //iterates as long as all taxa are separated
            while (minimalPartLevel < numTaxa) {
                System.out.println(new Date().toString());
                System.out.println(minimalPartLevel + " of " + numTaxa + " done!");
                System.out.println("Number of solutions alive: " + partitions.size());
                System.out.println();
                final Set<Partition> allNewPartitionsSet = new HashSet<>();
                //start with best partitions...
                long upperBound = Long.MAX_VALUE;
                while (!partitions.isEmpty()) {
                    LinkedList<Partition> part = partitions.poll().getKBestNew(numberOfCuts, upperBound);

                    //actualise upperbound
                    if (allNewPartitionsSet.addAll(part) && allNewPartitionsSet.size() >= numberOfCuts) {
                        long wannabeUpperBound = part.getLast().currentscore;
                        if (wannabeUpperBound < upperBound)
                            upperBound = wannabeUpperBound;
                    }
                }

                partitions.addAll(allNewPartitionsSet);
                Collections.sort(partitions);
                if (partitions.size() > (numberOfCuts))
                    partitions.subList(numberOfCuts + 1, partitions.size()).clear();
                minimalPartLevel = buildNextPartitionLevel(partitions, subsBench);
            }

            System.out.println("...DONE in " + ((double) (System.currentTimeMillis() - calctime) / 1000d) + "s");
            System.out.println();

            result = buildTreesFromPartitions(partitions);
        }
    }

    // this mehtod builds the new Partition list with respect to the subsBench ;-)
    private int buildNextPartitionLevel(final LinkedList<Partition> newPartitions, final TreeMap<Integer, Set<Partition>> subsBench) {
        int minimalPartSize = Integer.MAX_VALUE;

        if (!subsBench.isEmpty())
            minimalPartSize = subsBench.firstKey();

        for (Partition partition : newPartitions) {
            if (partition.getSize() < minimalPartSize)
                minimalPartSize = partition.getSize();
        }

        Iterator<Partition> partIt = newPartitions.iterator();
        while (partIt.hasNext()) {
            Partition partition = partIt.next();

            if (partition.getSize() > minimalPartSize) {
                partIt.remove();
                if (subsBench.containsKey(partition.getSize())) {
                    subsBench.get(partition.getSize()).add(partition);
                } else {
                    HashSet<Partition> p = new HashSet<Partition>();
                    p.add(partition);
                    subsBench.put(partition.getSize(), p);
                }
            }
        }
        Set<Partition> p = subsBench.get(minimalPartSize);
        subsBench.remove(minimalPartSize);
        if (p != null)
            newPartitions.addAll(p);

        Collections.sort(newPartitions);
        if (newPartitions.size() > (numberOfCuts))
            newPartitions.subList(numberOfCuts, newPartitions.size()).clear();

        return minimalPartSize;
    }

    private List<Tree> buildTreesFromPartitions(List<Partition> partitions) {
        //this ist just to build the supertree edgelist finally!
        long supertreetime = System.currentTimeMillis();
        System.out.println("Builing Supertrees...");
        int treeNumber = 1;
        List<Tree> supertrees = new ArrayList<>(numberOfCuts);
        for (Partition partition : partitions) {
            //build the supertree from this partition..
            partition.getKBestNew(numberOfCuts, -1l); //needed to remove the single graphs
            final Tree s = partition.buildTree();
            System.out.println("SupertreeScore = " + s.getName());
            supertrees.add(s);
            treeNumber++;
        }
        System.out.println("...DONE in " + ((double) (System.currentTimeMillis() - supertreetime) / 1000d) + "s");
        return supertrees;
    }

    @Override
    protected FlipCutGraphMultiSimpleWeight createInitGraph(CostComputer costsComputer) {
        return new FlipCutGraphMultiSimpleWeight(costsComputer, numberOfCuts, type);
    }

    //this method contains only simple weightings //todo redundant with singlecut version..., conceptional proble,  make better!
    @Override
    protected CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree) {
        CostComputer costs = null;
        if (UnitCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            LOGGER.info("Using Unit Costs");
            costs = new UnitCostComputer(inputTrees, scaffoldTree);
        } else if (WeightCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            costs = new WeightCostComputer(inputTrees, weights, scaffoldTree);
            LOGGER.info("Using " + weights);
        } else {
            LOGGER.warning("No supported weight option set. Setting to standard: " + FlipCutWeights.Weights.EDGE_AND_LEVEL);
            setWeights(FlipCutWeights.Weights.EDGE_AND_LEVEL);
            initCosts(inputTrees, scaffoldTree);
        }
        return costs;
    }


    public int getNumberOfCuts() {
        return numberOfCuts;
    }

    public void setNumberOfCuts(int numberOfCuts) {
        this.numberOfCuts = numberOfCuts;
    }

    @Override
    public Tree getResult() {
        if (result == null || result.isEmpty())
            return null;
        return getResults().get(0);
    }

    @Override
    public List<Tree> getResults() {
        if (result == null || result.isEmpty())
            return null;
        return result;
    }

    @Override
    public Algorithm<List<Tree>, Tree> call() {
        calculateSTs();
        return this;
    }
}
