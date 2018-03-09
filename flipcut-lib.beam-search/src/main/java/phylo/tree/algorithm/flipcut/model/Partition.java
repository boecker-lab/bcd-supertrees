
package phylo.tree.algorithm.flipcut.model;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import mincut.cutGraphAPI.bipartition.MultiCut;
import org.jetbrains.annotations.NotNull;
import phylo.tree.algorithm.flipcut.SourceTreeGraphMultiCut;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author Markus Fleischauer (markus.fleischauer@uni-jena.de)
 * Date: 17.01.13
 * Time: 17:49
 */

public class Partition implements Comparable<Partition> {
    public final long currentscore;
    public final int cachedHash;
    public final AtomicInteger treeNodeIndex;

    private final Map<SourceTreeGraphMultiCut, Edge> graphs;
    private List<Edge> supertreeEdges = new LinkedList<>();
    private int finishedGraphs;

    public Partition(SourceTreeGraphMultiCut initialGraph) {
        currentscore = 0;
        finishedGraphs = 0;
        treeNodeIndex = new AtomicInteger(0);

        graphs = new HashMap<>();
        graphs.put(initialGraph, new Edge(0, treeNodeIndex.incrementAndGet()));
        cachedHash = this.graphs.keySet().hashCode();
    }

    private Partition(long score, Map<SourceTreeGraphMultiCut, Edge> graphs, List<Edge> edges, int finished, final AtomicInteger treeNodeIndex) {
        currentscore = score;
        finishedGraphs = 0;

        this.graphs = graphs;
        cachedHash = this.graphs.hashCode();
        supertreeEdges = edges;
        finishedGraphs = finished;
        this.treeNodeIndex = treeNodeIndex;
    }

    /*
     * This method constructs the k best new partitions based on this
     * Attention this may calculate time critical minimum Cuts
     */
    public LinkedList<Partition> getKBestNew(int k, long upperBound) {
        PriorityQueue<MultiCut> cutsDesc = new PriorityQueue<>(k, new Comparator<MultiCut>() {
            public int compare(MultiCut o1, MultiCut o2) {
                return -o1.compareTo(o2);
            }
        });

        List<Iterator<MultiCut>> graphIterList = new LinkedList<>();
        List<SourceTreeGraphMultiCut> toRemove = new LinkedList<>();
        for (SourceTreeGraphMultiCut graph : graphs.keySet()) {
            assert graph.numTaxa() > 1 : "Error: Graph of size <= 1 shouldn't be possible!!!"; //only 1 taxon left --> labeling node corresponding to the graph with the label of the last taxon

            //delete semi universals
            if (!graph.containsCuts())
                graph.deleteSemiUniversals();
            //get first partition of every graph to preselect and save time and memory
            Iterator<MultiCut> iter = graph.getCutIterator();
            MultiCut c = iter.next();
            //check if this partition can be better than one outside
            if ((c.minCutValue() + currentscore) < upperBound) {
                //add the graphs that have a chance
                if (cutsDesc.size() >= k) {
                    if (c.minCutValue() < cutsDesc.peek().minCutValue()) {
                        cutsDesc.add(c);
                        graphIterList.add(iter);
                        //remove last cut
                        cutsDesc.poll();
                    }
                } else {
                    cutsDesc.add(c);
                    graphIterList.add(iter);
                }
            }
        }

        for (SourceTreeGraphMultiCut g : toRemove) {
            supertreeEdges.add(graphs.remove(g));
        }

        //find the k-best mincut of all k^2
        while (!graphIterList.isEmpty()) {
            Iterator<Iterator<MultiCut>> graphIter = graphIterList.iterator();
            while (graphIter.hasNext()) {
                Iterator<MultiCut> cutIterator = graphIter.next();
                while (cutIterator.hasNext()) {
                    MultiCut cut = cutIterator.next();
                    //check upper bound
                    if ((cut.minCutValue() + currentscore) < upperBound) {
                        //check if better than the k we have
                        if (cutsDesc.size() >= k) {
                            if (cut.minCutValue() < cutsDesc.peek().minCutValue()) {
                                cutsDesc.add(cut);
                                //remove last cut
                                cutsDesc.poll();
                            } else {
                                break;
                            }
                        } else {
                            cutsDesc.add(cut);
                        }
                    }
                }
                graphIter.remove();
            }
        }
        //convert to ascending list
        List<MultiCut> cuts = new LinkedList<>(cutsDesc);
        Collections.sort(cuts);

        // build the k best partitions
        LinkedList<Partition> partitions = new LinkedList<>();
        for (MultiCut cut : cuts) {
            //add new edge for cutted graph to supertree edgeset
            List<SourceTreeGraphMultiCut> splittedGraphs = cut.getSplittedGraphs();

            //edges for new partition
            List<Edge> edges = new LinkedList<>(supertreeEdges);

            //build new partition
            Map<SourceTreeGraphMultiCut, Edge> newPartitionGraphs = new HashMap<>(graphs);
            Edge sourceGraphEdge = newPartitionGraphs.remove(cut.sourceGraph());
            edges.add(sourceGraphEdge);


            //check if one of the splitted graphes is finished
            int newFinished = finishedGraphs;
            Iterator<SourceTreeGraphMultiCut> it = splittedGraphs.iterator();
            while (it.hasNext()) {
                SourceTreeGraphMultiCut splitGraph = it.next();
                Edge splitGraphEdge = new Edge(sourceGraphEdge.treeNode, treeNodeIndex.incrementAndGet());
                final int taxaNum = splitGraph.numTaxa();
                assert taxaNum >= 0 : "Error: empty graph in partition";
                if (taxaNum == 1) {
                    splitGraphEdge.treeNodeLabel = (String) splitGraph.taxaLabels().iterator().next();
                    edges.add(splitGraphEdge);
                    newFinished++;
                } else {
                    newPartitionGraphs.put(splitGraph, splitGraphEdge);
                }
            }

            Partition p = new Partition(currentscore + cut.minCutValue(), newPartitionGraphs, edges, newFinished, treeNodeIndex);
            partitions.add(p);
        }
        return partitions;
    }

    public int getSize() {
        return graphs.size() + finishedGraphs;
    }

    public int getNumOfGraphs() {
        return graphs.size();
    }

    public int compareTo(@NotNull Partition o) {
        return Long.compare(currentscore, o.currentscore);
    }

    public Tree buildTree() {
        TIntObjectMap<TreeNode> nodeMap = new TIntObjectHashMap<>();
        Tree tree = new Tree();

        Iterator<Edge> it = supertreeEdges.iterator();
        while (it.hasNext()) {
            Edge edge = it.next();
            nodeMap.put(edge.treeNode, new TreeNode(edge.treeNodeLabel));
            tree.addVertex(nodeMap.get(edge.treeNode));
            if (edge.parentNode == 0) {
                tree.setRoot(nodeMap.get(edge.treeNode));
                it.remove();
            }
        }

        for (Edge edge : supertreeEdges) {
            tree.addEdge(nodeMap.get(edge.parentNode), nodeMap.get(edge.treeNode));
        }

        tree.setName(String.valueOf(currentscore));
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
        return !graphs.isEmpty() && !p2.graphs.isEmpty() && cachedHash == p2.cachedHash;
    }

    class Edge {
        final int parentNode;
        final int treeNode;
        String treeNodeLabel = null;

        Edge(int parentNode, int treeNode) {
            this.parentNode = parentNode;
            this.treeNode = treeNode;
        }
    }
}

