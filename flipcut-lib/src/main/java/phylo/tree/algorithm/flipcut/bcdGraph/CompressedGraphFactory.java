package phylo.tree.algorithm.flipcut.bcdGraph;

import gnu.trove.impl.Constants;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phylo.tree.algorithm.flipcut.costComputer.CostComputer;
import phylo.tree.algorithm.flipcut.cutter.CutGraphCutter;
import phylo.tree.model.Tree;
import phylo.tree.model.TreeNode;
import phylo.tree.model.TreeUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CompressedGraphFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompressedGraphFactory.class);

    public static CompressedBCDSourceGraph createSourceGraph(CostComputer costComputer, double bootstrapTheshold) {
        System.out.println("Creating graph representation of input trees...");
        final Tree scaffold = costComputer.getScaffoldTree();
        final List<Tree> trees = costComputer.getTrees();

        int leafIndex = 0;
        AtomicInteger characterIndex = new AtomicInteger(0);

        TIntObjectMap<RoaringBitmap> scaffoldMapping = new TIntObjectHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
        TObjectIntMap<String> leafs = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);


        Map<Set<String>, Hyperedge> edges = new LinkedHashMap<>();
        Map<Set<String>, RoaringBitmap> duplicates = new HashMap<>();

        int allChars = 0;

        LOGGER.info("Reading trees...");
        RoaringBitmap activeScaffoldCharacters = null;
        if (scaffold != null) {
            TreeNode scaffoldRoot = scaffold.getRoot();
            Set<String> treeTaxa = new HashSet<>();

            for (TreeNode scaffoldNode : scaffold.vertices()) {
                if (scaffoldNode.isLeaf()) {
                    leafs.put(scaffoldNode.getLabel(), leafIndex++);
//                    treeTaxa.add(leafIndex++);
                    treeTaxa.add(scaffoldNode.getLabel());
                }else {
                    allChars++;
                }
            }

            activeScaffoldCharacters = addScaffoldCharacterRecursive(scaffoldRoot.getChildren(), characterIndex, costComputer, treeTaxa, leafs, duplicates, edges, scaffoldMapping);
        }
        if (activeScaffoldCharacters == null) activeScaffoldCharacters = new RoaringBitmap();


        //do character stuff
        for (Tree tree : trees) {
            if (tree != scaffold) {
                TreeNode root = tree.getRoot();
                Set<String> treeTaxa = new HashSet<>(tree.vertexCount());
                List<TreeNode> inner = new LinkedList<>();

                for (TreeNode node : root.depthFirstIterator()) {
                    if (node.isLeaf()) {
                        if (scaffold == null && leafs.putIfAbsent(node.getLabel(), leafIndex) == leafs.getNoEntryValue())
                            leafIndex++;
                        treeTaxa.add((node.getLabel()));
                    } else if (!node.equals(root)) {
                        inner.add(node);
                    }
                }
                allChars += inner.size() ;

                for (TreeNode node : inner) {
                    //collect no edges and zero edges
                    Hyperedge edge = addEdge(node, treeTaxa, leafs, duplicates, edges, costComputer);
                    if (edge.isInfinite())
                        System.out.println("guide tree edge overlaps with normal one: ");
                }
            }
        }


        assert leafs.size() == leafIndex;


        //chreate chars

        int charIndex = 0;
        final Hyperedge[] hyperedges = new Hyperedge[edges.size()];
        LOGGER.info("Add " + edges.size() + " merged Characters to graph...");
        int allMergedChars = 0;
        for (Hyperedge hyperedge : edges.values()) {
            hyperedges[charIndex++] = hyperedge;
            allMergedChars += hyperedge.umergedNumber();
        }
        LOGGER.info(allChars + " where merged to " + allMergedChars + " and can be further reduced to " + edges.size() + " during mincut");


        //create taxa;
        final String[] taxa = new String[leafs.size()];
        LOGGER.info("Add " + taxa.length + " Taxa to graph...");
        leafs.forEachEntry((a, b) -> {
            taxa[b] = a;
            return true;
        });

        return new CompressedBCDSourceGraph(taxa, hyperedges, activeScaffoldCharacters, scaffoldMapping);
    }

    private static TIntSet createBits(TreeNode node, final TObjectIntMap<String> leafs) {
        TIntSet oneBits = new TIntHashSet();
        for (TreeNode treeNode : node.depthFirstIterator()) {
            if (treeNode.isLeaf()) {
                oneBits.add(leafs.get(treeNode.getLabel()));
            }
        }
        return oneBits;
    }

    private static RoaringBitmap getCompressedBits(final Map<Set<String>, RoaringBitmap> zs, Set<String> bits, final TObjectIntMap<String> leafs) {
        RoaringBitmap cbits = zs.get(bits);
        if (cbits == null) {
            cbits = new RoaringBitmap();
            for (String s : bits) {
                cbits.add(leafs.get(s));
                zs.put(bits, cbits);
            }
        }
        return cbits;
    }

    private static RoaringBitmap getCompressedBits(final Map<TIntSet, RoaringBitmap> zs, TIntSet bits) {
        RoaringBitmap edge = zs.get(bits);
        if (edge == null) {
            edge = RoaringBitmap.bitmapOf(bits.toArray());
            zs.put(bits, edge);
        }
        return edge;
    }


    private static Hyperedge addEdge(final TreeNode node, final Set<String> treeTaxa, final TObjectIntMap<String> leafs, final Map<Set<String>, RoaringBitmap> zs, final Map<Set<String>, Hyperedge> edges, final CostComputer costComputer) {
        Set<String> oneBits = TreeUtils.getLeafLabels(node);

        Hyperedge hyperedge = edges.get(oneBits);
        if (hyperedge == null) {
            RoaringBitmap edge = getCompressedBits(zs, oneBits, leafs);
            hyperedge = new Hyperedge(edge);
            edges.put(oneBits, hyperedge);
        }
        Set<String> zeroBits = new HashSet<>(treeTaxa);
        zeroBits.removeAll(oneBits);

        hyperedge.addZero(
                getCompressedBits(zs, zeroBits, leafs),
                costComputer.getEdgeWeight(node)
        );

        return hyperedge;
    }


    private static RoaringBitmap addScaffoldCharacterRecursive(final List<TreeNode> children, final AtomicInteger charIndex,
                                                               final CostComputer costComputer,
                                                               final Set<String> treeTaxa,
                                                               TObjectIntMap<String> leafs,
                                                               final Map<Set<String>, RoaringBitmap> zs,
                                                               final Map<Set<String>, Hyperedge> edges,
                                                               final TIntObjectMap<RoaringBitmap> scaffoldMapping) {

        RoaringBitmap childrenI = new RoaringBitmap();

        for (TreeNode scaffoldNode : children) {
            if (scaffoldNode.isInnerNode()) {
                //create hyperedge
                Hyperedge hyperedge = addEdge(scaffoldNode, treeTaxa, leafs, zs, edges, costComputer);

                assert hyperedge.getWeight() == CutGraphCutter.getInfinity();

                childrenI.add(charIndex.get());

                RoaringBitmap set = addScaffoldCharacterRecursive(scaffoldNode.getChildren(), charIndex, costComputer, treeTaxa, leafs, zs, edges, scaffoldMapping);
                if (!set.isEmpty())
                    scaffoldMapping.put(charIndex.getAndIncrement(), set);
                charIndex.incrementAndGet();
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
