
package flipCut.model;

import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import flipCut.flipCutGraph.FlipCutGraphMultiSimpleWeight;

import java.util.*;


/**
 * @Author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 17:49
 */

public class Partition implements Comparable<Partition> {
    public final long currentscore;
    public final int cachedHash;

    public final Set<FlipCutGraphMultiSimpleWeight> graphs; //todo back to private
    private TreeNode root;
    private Set<Edge> supertreeEdges = new HashSet<>();
    private int finishedGraphs;


    public Partition(long score, FlipCutGraphMultiSimpleWeight graph) {
        currentscore = score;
        finishedGraphs = 0;

        graphs = new HashSet<>();
        graphs.add(graph);
        cachedHash = this.graphs.hashCode();
        root = graph.treeNode;
    }

    public Partition(long score, Set<FlipCutGraphMultiSimpleWeight> graphs, TreeNode root, Set<Edge> edges, int finished) {
        currentscore = score;
        finishedGraphs = 0;

        this.graphs = graphs;
        cachedHash = this.graphs.hashCode();
        this.root = root;
        supertreeEdges = edges;
        finishedGraphs = finished;
    }

    /*This method constructs the k best new partitions based on this
    * Attention this may be calculates time critical minimum Cuts*/
    //todo parallelize this step
    public List<Partition> getKBestNew(int k, long upperBound) {
        PriorityQueue<Cut> cutsDesc = new PriorityQueue<Cut>(k,new Comparator<Cut>() {
            public int compare(Cut o1, Cut o2) {
                return - o1.compareTo(o2);
            }
        });

        List<Iterator<Cut>> graphIterList = new LinkedList<Iterator<Cut>>();
        List<FlipCutGraphMultiSimpleWeight> toRemove = new LinkedList<FlipCutGraphMultiSimpleWeight>();
        for (FlipCutGraphMultiSimpleWeight graph : graphs) {
            //only 1 taxon left --> labeling node corresponding to the graph with the label of the last taxon
            if (graph.taxa.size() == 1) {
                graph.treeNode.setLabel(graph.taxa.get(0).name);
                toRemove.add(graph);
                finishedGraphs++;
                System.out.println("WARNING: shouldn't be possible anymore!!! or?!"); //todo remove this if sure
                //more than 1 taxa left --> cut graph to find split
            } else {
                //delete semi universals
                if (!graph.containsCuts())
                    graph.deleteSemiUniversals();
                //get first partition of every graph to preselect and save time and memory
                Iterator<Cut> iter = graph.getCutIterator();
                Cut c = iter.next();
                //check if this partition can be better than one outside
                if ((c.minCutValue + currentscore) < upperBound) {
                    //add the graphs that have a chance
                    if (cutsDesc.size() >= k) {
                        if (c.minCutValue < cutsDesc.peek().minCutValue){
                            cutsDesc.add(c);
                            graphIterList.add(iter);
                            //remove last cut
                            cutsDesc.poll();
                        }
                    }else{
                        cutsDesc.add(c);
                        graphIterList.add(iter);
                    }
                }
            }
        }
        graphs.removeAll(toRemove);

        //find the k-best cuts of all k^2
        while (!graphIterList.isEmpty()) {
            Iterator<Iterator<Cut>> graphIter = graphIterList.iterator();
            while (graphIter.hasNext()) {
                Iterator<Cut> cutIterator = graphIter.next();
                while (cutIterator.hasNext()) {
                    Cut cut = cutIterator.next();
                    //check upper bound
                    if ((cut.minCutValue + currentscore) < upperBound) {
                            //check if better than the k we have
                        if (cutsDesc.size() >= k) {
                            if (cut.minCutValue < cutsDesc.peek().minCutValue) {
                                cutsDesc.add(cut);
                                //remove last cut
                                cutsDesc.poll();
                            } else {
                                break;
                            }
                        }else{
                            cutsDesc.add(cut);
                        }
                    }
                }
                graphIter.remove();
            }
        }
        //convert to ascending list
        List<Cut> cuts = new LinkedList<Cut>(cutsDesc);
        Collections.sort(cuts);

        // build the k best partitions
        List<Partition> partitions = new LinkedList<>();
        for (Cut cut : cuts) {
            //add new edge for cutted graph to supertree edgeset
            List<FlipCutGraphMultiSimpleWeight> splittedGraphs = cut.getSplittedGraphs();

            Set<Edge> edges = new HashSet<Edge>(supertreeEdges.size() + splittedGraphs.size());
            edges.addAll(supertreeEdges);

            for (FlipCutGraphMultiSimpleWeight g : splittedGraphs) {
                edges.add(new Edge(g.parentNode, g.treeNode));
            }


            //build new partition
            Set<FlipCutGraphMultiSimpleWeight> newPartitionGraphs = new HashSet<>(graphs);
            newPartitionGraphs.remove(cut.sourceGraph);

            //check if one of the splitted graphes is finished
            int newFinished = finishedGraphs;
            Iterator<FlipCutGraphMultiSimpleWeight> it = splittedGraphs.iterator();
            while (it.hasNext()) {
                FlipCutGraphMultiSimpleWeight splitGraph = it.next();
                if (splitGraph.taxa.size() == 1) {
                    splitGraph.treeNode.setLabel(splitGraph.taxa.get(0).name); //todo if or not
                    newFinished++;
                }else{
                    newPartitionGraphs.add(splitGraph);
                }
            }

            Partition p = new Partition(currentscore + cut.minCutValue, newPartitionGraphs, root, edges, newFinished);
            partitions.add(p);
        }
        return partitions;
    }

    public int getSize() {
        return graphs.size() + finishedGraphs;
    }

    public int compareTo(Partition o) {
        return (currentscore < o.currentscore) ? -1 : ((currentscore == o.currentscore) ? 0 : 1);
    }

    public Tree createSupertree(int treeNumber){
        Map<TreeNode,TreeNode> nodeMap = new HashMap<>();
        Tree tree = new Tree();
        nodeMap.put(root, new TreeNode(root.getLabel()));
        tree.addVertex(nodeMap.get(root));
        for (Edge edge : supertreeEdges) {
            nodeMap.put(edge.target, new TreeNode(edge.target.getLabel()));
            tree.addVertex(nodeMap.get(edge.target));
        }
        for (Edge edge : supertreeEdges) {
            tree.addEdge(nodeMap.get(edge.source),nodeMap.get(edge.target));
        }
        tree.setName("Tree" + treeNumber + "_" + currentscore);
        return tree;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Partition partition = (Partition) o;

        return compareGraphs(partition);
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }

    public boolean compareGraphs(Partition p2) {
        if (graphs.isEmpty() || p2.graphs.isEmpty())
            return false; //todo proof!!!
        return cachedHash == p2.cachedHash;
    }

    class Edge {
        TreeNode source;
        TreeNode target;

        Edge(TreeNode source, TreeNode target) {
            this.source = source;
            this.target = target;
        }
    }
}

