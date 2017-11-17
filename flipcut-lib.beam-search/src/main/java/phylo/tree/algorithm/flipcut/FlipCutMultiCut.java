package phylo.tree.algorithm.flipcut;

import core.algorithm.Algorithm;
import phylo.tree.algorithm.flipcut.cutter.MultiCutter;
import phylo.tree.algorithm.flipcut.cutter.MultiCutterFactory;
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
public class FlipCutMultiCut<S, T extends SourceTreeGraphMultiCut<S, T>, C extends MultiCutter<S, T>> extends AbstractFlipCut<S, T, C, MultiCutterFactory<C, S, T>> {
    protected int numberOfCuts = 1;
    protected List<Tree> result;

    //todo set type to null

    public FlipCutMultiCut() {
        super();
    }

    @Override
    protected String name() {
        return getClass().getSimpleName();
    }

    public FlipCutMultiCut(MultiCutterFactory<C, S, T> type) {
        super(type);
    }

    public FlipCutMultiCut(Logger log, MultiCutterFactory<C, S, T> type) {
        super(log, type);
    }

    public FlipCutMultiCut(Logger log, ExecutorService executorService, MultiCutterFactory<C, S, T> type) {
        super(log, executorService, type);
    }

    private void calculateSTs() {
        //init fields
        result = null;


        if (initialGraph != null) {
            long calctime = System.currentTimeMillis();
            //map to store partitions which are already cutted in more parts than the others
            final TreeMap<Integer, Set<Partition>> subsBench = new TreeMap<>();
            final int numTaxa = initialGraph.numTaxa();

            System.out.println("Calculating Partitions...");

            //initial step to generate
            final LinkedList<Partition> partitions = new Partition(initialGraph).getKBestNew(numberOfCuts, Long.MAX_VALUE);
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
        Set<Partition> p = subsBench.remove(minimalPartSize);
        if (p != null) {
            newPartitions.addAll(p);
        }

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
