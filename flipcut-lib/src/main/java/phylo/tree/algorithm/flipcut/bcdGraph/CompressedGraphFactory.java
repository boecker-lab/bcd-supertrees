package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.impl.Constants;
import gnu.trove.list.TLongList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CompressedGraphFactory {

    public static CompressedBCDSourceGraph createSourceGraph(CostComputer costComputer, double bootstrapTheshold) {

        final Tree scaffold = costComputer.getScaffoldTree();
        final List<Tree> trees = costComputer.getTrees();

        int leafIndex = 0;
        AtomicInteger characterIndex = new AtomicInteger(0);

        TIntObjectMap<RoaringBitmap> scaffoldMapping = new TIntObjectHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
        TObjectIntMap<String> leafs = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);


        Map<TIntSet, Hyperedge> edges = new LinkedHashMap<>();
        Map<TIntSet, RoaringBitmap> duplicates = new HashMap<>();


        RoaringBitmap activeScaffoldCharacters = null;
        if (scaffold != null) {
            TreeNode scaffoldRoot = scaffold.getRoot();
            TIntSet treeTaxa = new TIntHashSet();

            for (TreeNode scaffoldNode : scaffold.vertices()) {
                if (scaffoldNode.isLeaf()) {
                    leafs.put(scaffoldNode.getLabel(), leafIndex);
                    treeTaxa.add(leafIndex++);
                }
            }

            activeScaffoldCharacters = addChildren(scaffoldRoot.getChildren(), characterIndex, costComputer, treeTaxa, leafs, duplicates, edges, scaffoldMapping);
        }


        //do character stuff
        for (Tree tree : trees) {
            if (tree != scaffold) {
                TreeNode root = tree.getRoot();
                TIntSet treeTaxa = new TIntHashSet(tree.vertexCount());
                List<TreeNode> inner = new LinkedList<>();

                for (TreeNode node : root.depthFirstIterator()) {
                    if (node.isLeaf()) {
                        if (scaffold == null && leafs.putIfAbsent(node.getLabel(), leafIndex) == leafs.getNoEntryValue())
                            leafIndex++;
                        treeTaxa.add(leafs.get(node.getLabel()));
                    } else if (!node.equals(root)) {
                        inner.add(node);
                    }
                }

                for (TreeNode node : inner) {
                    //collect no edges and zero edges
                    Hyperedge edge = addEdge(node,treeTaxa,leafs,duplicates,edges,costComputer);
                    if (edge.isInfinite())
                        System.out.println("guide tree edge overlaps with normal one: ");
                }
            }
        }


        assert leafs.size() == leafIndex;


        //chreate chars
        int charIndex = 0;
        final Hyperedge[] hyperedges = new Hyperedge[edges.size()];
        for (Hyperedge hyperedge : edges.values()) {
            hyperedges[charIndex++] = hyperedge;
        }

        //create taxa;
        final String[] taxa = new String[leafs.size()];
        leafs.forEachEntry((a, b) -> {
            taxa[b] = a;
            return true;
        });

        return new CompressedBCDSourceGraph(taxa, hyperedges, activeScaffoldCharacters, scaffoldMapping);
    }

    private static Hyperedge addEdge(final TreeNode node, final TIntSet treeTaxa, final TObjectIntMap<String> leafs, final Map<TIntSet, RoaringBitmap> zs, final Map<TIntSet, Hyperedge> edges, final CostComputer costComputer) {
        TIntSet oneBits = new TIntHashSet();
        for (TreeNode treeNode : node.depthFirstIterator()) {
            if (treeNode.isLeaf()) {
                oneBits.add(leafs.get(treeNode.getLabel()));
            }
        }

        Hyperedge hyperedge = edges.get(oneBits);
        if (hyperedge == null) {
            RoaringBitmap edge = getCompressedBits(zs, oneBits);
            hyperedge = new Hyperedge(edge);
            edges.put(oneBits, hyperedge);
        }
        TIntHashSet zeroBits = new TIntHashSet(treeTaxa);
        zeroBits.removeAll(oneBits);

        hyperedge.addZero(
                getCompressedBits(zs, zeroBits),
                costComputer.getEdgeWeight(node)
        );

        return hyperedge;
    }

    private static RoaringBitmap getCompressedBits(final Map<TIntSet, RoaringBitmap> zs, TIntSet bits) {
        RoaringBitmap edge = zs.get(bits);
        if (edge == null) {
            edge = RoaringBitmap.bitmapOf(bits.toArray());
            zs.put(bits, edge);
        }
        return edge;
    }

    private static RoaringBitmap addChildren(final List<TreeNode> children, final AtomicInteger charIndex,
                                             final CostComputer costComputer,
                                             final TIntSet treeTaxa,
                                             TObjectIntMap<String> leafs,
                                             final Map<TIntSet, RoaringBitmap> zs,
                                             final Map<TIntSet, Hyperedge> edges,
                                             final TIntObjectMap<RoaringBitmap> scaffoldMapping) {

        RoaringBitmap childrenI = new RoaringBitmap();

        for (TreeNode scaffoldNode : children) {
            if (scaffoldNode.isInnerNode()) {
                //create hyperedge
                Hyperedge hyperedge = addEdge(scaffoldNode, treeTaxa, leafs, zs, edges, costComputer);

                assert hyperedge.getWeight() == CutGraphCutter.getInfinity();

                childrenI.add(charIndex.get());
                scaffoldMapping.put(charIndex.getAndIncrement(),
                        addChildren(scaffoldNode.getChildren(), charIndex, costComputer, treeTaxa, leafs, zs, edges, scaffoldMapping));
            }
        }

        return childrenI;
    }


    private static class Character {
        final RoaringBitmap ones;
        final RoaringBitmap zeroes;
        final int hash;

        public Character(RoaringBitmap ones, RoaringBitmap zeroes) {
            this.ones = ones;
            this.zeroes = zeroes;
            hash = calcHash();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Character)) return false;
            Character character = (Character) o;
            return ones == character.ones && zeroes == character.zeroes;
            /*if (hash != character.hash) return false;

            if (!ones.equals(character.ones)) return false;
            return zeroes.equals(character.zeroes);*/
        }

        private int calcHash() {
            int result = ones.hashCode();
            result = 31 * result + zeroes.hashCode();
            return result;
        }


        @Override
        public int hashCode() {
            return hash;
        }
    }

    /*private static class CachedRoaringBitmap {
        final RoaringBitmap source;
        final int hash;

        public CachedRoaringBitmap(RoaringBitmap source) {
            this.source = source;
            hash = source.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CachedRoaringBitmap)) return false;

            CachedRoaringBitmap that = (CachedRoaringBitmap) o;
            if (hash != that.hash) return false;

            return source.equals(that.source);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }*/

}
