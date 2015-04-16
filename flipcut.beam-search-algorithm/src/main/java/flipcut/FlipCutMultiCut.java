package flipcut;

import epos.model.tree.Tree;
import flipcut.costComputer.CostComputer;
import flipcut.costComputer.FlipCutWeights;
import flipcut.costComputer.UnitCostComputer;
import flipcut.costComputer.WeightCostComputer;
import flipcut.flipCutGraph.AbstractFlipCutGraph;
import flipcut.flipCutGraph.CutGraphCutter;
import flipcut.flipCutGraph.FlipCutGraphMultiSimpleWeight;
import flipcut.flipCutGraph.FlipCutNodeSimpleWeight;
import flipcut.model.Partition;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 13:56
 */
public class FlipCutMultiCut extends AbstractFlipCut<FlipCutNodeSimpleWeight,FlipCutGraphMultiSimpleWeight> {
    protected int numberOfCuts = 1;
    private List<Partition> partitions;
    //map to store partitions which are already cutted in more parts than the others
    private final TreeMap<Integer,Set<Partition>> subsBench = new TreeMap<>();
    final CutGraphCutter.CutGraphTypes type;

    public FlipCutMultiCut(CutGraphCutter.CutGraphTypes type) {
        this.type = type;
    }

    public FlipCutMultiCut(Logger log, CutGraphCutter.CutGraphTypes type) {
        super(log);
        this.type = type;
    }

    @Override
    public List<Tree> getSupertrees() {

        List<Tree> supertrees = new ArrayList<>(numberOfCuts);

        if (currentGraph != null) {
            long calctime = System.currentTimeMillis();
            System.out.println("Calculating Partitions...");

            final int numTaxa = currentGraph.taxa.size();

            //initial step to generate

            partitions =  new Partition(0, currentGraph).getKBestNew(numberOfCuts, Long.MAX_VALUE);
            currentGraph = null; //get rid of these large graph

            int partitionningSteps = 0; //DEBUG variable
            if (DEBUG) {
                System.out.println("##### Step: " + partitionningSteps + " #####");
                System.out.println("NUM Partitions before: " + partitions.size() + "");
            }

            int minimalPartSize = buildPartitionList(partitions);
            if (DEBUG)
                System.out.println("NUM Partitions after sorting: " + partitions.size() + "");


            Set<AbstractFlipCutGraph> allGraphsDebug =  new HashSet<>();
            int numAllGraphPointer = 0;
            //iterates as long as all taxa are separated
            while (minimalPartSize < numTaxa) {
                if (DEBUG) {
                    partitionningSteps++;
                    System.out.println();
                    System.out.println();
                    System.out.println("##### Step: " + partitionningSteps + " #####");
                    System.out.println("NUM Partitions before: " + partitions.size() + "");
                }


                final Set<Partition> allNewPartitionsSet = new HashSet<>();
                //start with best partitions...
                int counter = 0;
                long upperBound = Long.MAX_VALUE;
                for (Partition partition : partitions) {
                    counter++;
                    List<Partition> part = partition.getKBestNew(numberOfCuts,upperBound);

                    //todo remove debug stuff
                    int s = allNewPartitionsSet.size();
                    if (!allNewPartitionsSet.addAll(part)) {
                        if (DEBUG)
                            System.out.println((part.size() - (allNewPartitionsSet.size() - s)) + " DUPLICATION(s)!!!!!!!!!!!!!!!!!!!!!!!");
                    }

                    if (counter >= numberOfCuts && (allNewPartitionsSet.size() >= (2 * numberOfCuts))){
                        if (DEBUG)
                            System.out.println(counter + " partitions used to get " + allNewPartitionsSet.size() + " new partitions");
                        break;
                    }

                    //actualise upperbound
                    if (!part.isEmpty()) {
                        long wannabeUpperBound = part.get(part.size() - 1).currentscore;
                        if (allNewPartitionsSet.size() >= (2 * numberOfCuts)) {
                            if (wannabeUpperBound < upperBound)
                                upperBound = wannabeUpperBound;
                        } else {
                            if (wannabeUpperBound > upperBound)
                                upperBound = wannabeUpperBound;
                        }
                    }
                }

                List<Partition> allNewPartitions = new LinkedList<>(allNewPartitionsSet);
                minimalPartSize = buildPartitionList(allNewPartitions);

                if (DEBUG) {
                    System.out.println("Progress: " + minimalPartSize + "/" + numTaxa);
                    System.out.println("NUM Partitions after sorting: " + partitions.size() + "");

                    //some more DEBUG shit
                    allGraphsDebug.clear();
                    numAllGraphPointer = 0;
                    for (Partition partition : partitions) {
                        allGraphsDebug.addAll(partition.graphs);
                        numAllGraphPointer += partition.graphs.size();
                    }
                    System.out.println("Number of Graphs = " + allGraphsDebug.size());
                    System.out.println("Number of GraphPointer = " + numAllGraphPointer);
                }




            }
            System.out.println("...DONE in " + ((double)(System.currentTimeMillis() - calctime)/1000d) + "s");
            System.out.println();

            //this ist just to build the supertree edgelist finally! //todo do in supertree building method!
            long supertreetime = System.currentTimeMillis();
            System.out.println("Builing Supertrees...");
            int treeNumber = 1;
            if (partitions.size() > numberOfCuts)
                partitions = partitions.subList(0,numberOfCuts); //todo output all trees?
            for (Partition partition : partitions) {
                //build the supertree from this partition..
                partition.getKBestNew(numberOfCuts,-1l); //needed to remove the single graphs
                supertrees.add(partition.createSupertree(treeNumber));
                treeNumber++;
            }
            System.out.println("...DONE in " + ((double)(System.currentTimeMillis() - supertreetime)/1000d) + "s");
        }
        return supertrees;
    }

    @Override
    protected FlipCutGraphMultiSimpleWeight createInitGraph(CostComputer costsComputer) {
        return new FlipCutGraphMultiSimpleWeight(costsComputer, numberOfCuts,type);
    }

    //this method contains only simple weightings //todo redundant with singlecut version... make better!
    @Override
    protected CostComputer initCosts(List<Tree> inputTrees, Tree scaffoldTree) {
        CostComputer costs = null;
        if (UnitCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            getLog().info("Using Unit Costs");
            costs = new UnitCostComputer(inputTrees,scaffoldTree);
        } else if (WeightCostComputer.SUPPORTED_COST_TYPES.contains(weights)) {
            costs = new WeightCostComputer(inputTrees,weights,scaffoldTree);
            getLog().info("Using " + weights);
        }else{
            getLog().warn("No supported weight option set. Setting to standard: "+ FlipCutWeights.Weights.EDGE_AND_LEVEL);
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

    // this mehtod builds the new Partition list with respect to the subsBench ;-)
    private int  buildPartitionList(List<Partition> newPartitions){
        int minimalPartSize = Integer.MAX_VALUE;

        Collections.sort(newPartitions);
        if (newPartitions.size() > (2*numberOfCuts))
            newPartitions = newPartitions.subList(0,(2*numberOfCuts));

        if (!subsBench.isEmpty())
            minimalPartSize = subsBench.firstKey();

        for (Partition partition : newPartitions) {
            if (partition.getSize() < minimalPartSize)
                minimalPartSize = partition.getSize();
        }

        Iterator<Partition> partIt = newPartitions.iterator();
        while (partIt.hasNext()) {
            Partition partition = partIt.next();

            if (partition.getSize() > minimalPartSize){
                partIt.remove();
                if (subsBench.containsKey(partition.getSize())){
                    subsBench.get(partition.getSize()).add(partition);
                }else{
                    HashSet<Partition> p = new HashSet<Partition>();
                    p.add(partition);
                    subsBench.put(partition.getSize(),p);
                }
            }
        }
        Set<Partition> p = subsBench.get(minimalPartSize);
        subsBench.remove(minimalPartSize);
        if (p != null)
            newPartitions.addAll(p);

        Collections.sort(newPartitions);

        partitions = newPartitions; //todo should be outside this method but how?
        return minimalPartSize;
    }
}
