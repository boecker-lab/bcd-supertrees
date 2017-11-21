package phylo.tree.algorithm.flipcut.bcdGraph;

import mincut.cutGraphAPI.bipartition.CompressedBCDCut;
import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;
import phylo.tree.algorithm.flipcut.SourceTreeGraph;
import phylo.tree.algorithm.flipcut.bcdGraph.edge.Hyperedge;
import phylo.tree.algorithm.flipcut.cutter.GraphCutter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class CompressedBCDGraph implements SourceTreeGraph<RoaringBitmap> {
    public final RoaringBitmap taxa;
    public final RoaringBitmap characters;
    protected final RoaringBitmap activeGuideEdges;

    protected CompressedBCDGraph(RoaringBitmap taxa, RoaringBitmap characters, RoaringBitmap activeGuideEdges) {
        this.taxa = taxa;
        this.characters = characters;
        this.activeGuideEdges = activeGuideEdges;
    }

    protected CompressedBCDGraph(RoaringBitmap characters, RoaringBitmap activeGuideEdges) {
        this.taxa = new RoaringBitmap();
        this.characters = characters;
        this.activeGuideEdges = activeGuideEdges;
    }

    //this is unchecked
    public Hyperedge getEdge(int hyperEdgeIndex) {
//        return getSource().sourceMergedHyperEdges[hyperEdgeIndex];
        return getSource().sourceMergedHyperEdges.get(hyperEdgeIndex);
    }

    //this is unchecked
    public String getTaxon(int taxonIndex) {
        return getSource().sourceTaxa[taxonIndex];
    }

    public abstract CompressedBCDSourceGraph getSource();


    public Iterable<String> taxaLabels() {
        return new ArrayBitMapIteratable<>(getSource().sourceTaxa, taxa);
    }

    public LinkedList<Hyperedge> getHyperEdgesAsList() {
        LinkedList<Hyperedge> l = new LinkedList<>();
        characters.forEach((IntConsumer) i -> {
            l.add(getEdge(i));
        });
        return l;
    }

    public Iterable<Hyperedge> hyperEdges() {
        return new IntMapBitMapIterable<>(getSource().sourceMergedHyperEdges, characters);
    }

    public boolean hasGuideEdges() {
        return activeGuideEdges != null && !activeGuideEdges.isEmpty();
    }

    public int numGuideEdges() {
        if (activeGuideEdges == null) return 0;
        return activeGuideEdges.getCardinality();
    }

    public Iterable<Hyperedge> guideHyperEdges() {
        return new IntMapBitMapIterable<>(getSource().sourceMergedHyperEdges, activeGuideEdges);
    }


    public int numTaxa() {
        return taxa.getCardinality();
    }

    public int numCharacter() {
        return characters.getCardinality();
    }

    public RoaringBitmap getConnectedComponent() {
//        long t = System.currentTimeMillis();
        LinkedList<Hyperedge> edges = getHyperEdgesAsList();

        RoaringBitmap component;
        if (!edges.isEmpty()) {
            component = edges.poll().ones().clone();
            boolean changed = true;
            while (changed) {
                changed = false;
                Iterator<Hyperedge> it = edges.iterator();
                while (it.hasNext()) {
                    RoaringBitmap currentEdge = it.next().ones();
                    if (RoaringBitmap.intersects(component, currentEdge)) {
                        component.or(currentEdge);
                        it.remove();
                        changed = true;
                    }
                }
            }
        } else {
            component = new RoaringBitmap();
            component.add(taxa.first());
        }
//        System.out.println("Connected Component in: " + (double) (System.currentTimeMillis() - t) / 1000d + "s");
        return component;
    }

    public void deleteSemiUniversals() {
        RoaringBitmap toDelete = new RoaringBitmap();
        characters.forEach((IntConsumer) i -> {
            if (getEdge(i).removeSemiuniversals(taxa)) {
                toDelete.add(i);
            }
        });

        deleteCharacters(toDelete);
    }

    public void deleteCharacters(RoaringBitmap toDelete) {
        deleteCharacters(toDelete, this);
    }

    public static CompressedBCDGraph cloneAndDeleteCharacters(RoaringBitmap toDelete, CompressedBCDGraph g) {
        CompressedBCDGraph clone = new CompressedBCDSubGraph(g.getSource(), g.taxa, g.characters.clone(), g.activeGuideEdges.clone());
        deleteCharacters(toDelete, clone);
        return clone;
    }

    public static void deleteCharacters(RoaringBitmap toDelete, CompressedBCDGraph g) {
        g.characters.xor(toDelete);
        RoaringBitmap guidesToDelete = RoaringBitmap.and(g.activeGuideEdges, toDelete);
        g.activeGuideEdges.xor(guidesToDelete);
        guidesToDelete.forEach((IntConsumer) key -> {
            RoaringBitmap nuGuideEdges = g.getSource().scaffoldCharacterHirarchie.get(key);
            if (nuGuideEdges != null)
                g.activeGuideEdges.or(nuGuideEdges);
        });
    }

    public List<CompressedBCDGraph> split() {
        List<CompressedBCDGraph> comps = new ArrayList<>();
        split(comps);
        return comps;
    }

    protected void split(final List<CompressedBCDGraph> graphs) {
        if (taxa.getCardinality() > 1) {
            RoaringBitmap connectedTaxa = getConnectedComponent();
            if (!connectedTaxa.equals(taxa)) {
                RoaringBitmap cCurrent = getCharacterForSubSetOfTaxa(connectedTaxa);
                RoaringBitmap guideCurrent = RoaringBitmap.and(cCurrent, activeGuideEdges);
                CompressedBCDSubGraph gCurrent = new CompressedBCDSubGraph(getSource(), connectedTaxa, cCurrent, guideCurrent);
                graphs.add(gCurrent);
                RoaringBitmap reverseTaxa = RoaringBitmap.xor(connectedTaxa, taxa);
                if (reverseTaxa.getCardinality() > 0) {
                    RoaringBitmap cRest = getCharacterForSubSetOfTaxa(reverseTaxa);
                    RoaringBitmap guideRest = RoaringBitmap.and(cRest, activeGuideEdges);
                    CompressedBCDSubGraph gRest = new CompressedBCDSubGraph(getSource(), reverseTaxa, getCharacterForSubSetOfTaxa(reverseTaxa), guideRest);
                    gRest.split(graphs);
                }
            } else {
                graphs.add(this);
            }
        } else {
            graphs.add(this);
        }
    }

    protected RoaringBitmap getCharacterForSubSetOfTaxa(RoaringBitmap taxa) {
        RoaringBitmap subCharacters = new RoaringBitmap();
        characters.forEach((IntConsumer) i -> {
            if (RoaringBitmap.intersects(getEdge(i).ones(), taxa)) {
                subCharacters.add(i);
            }
        });
        return subCharacters;
    }

    @Override
    public List<? extends SourceTreeGraph> getPartitions(GraphCutter c) {
        RoaringBitmap component = getConnectedComponent();
        if (isConnected(component)) {
            CompressedBCDCut cut = (CompressedBCDCut) c.cut(this);
            deleteCharacters(cut.getCutSet());
        }
        return split();
    }

    @Override
    public boolean isConnected() {
        return isConnected(getConnectedComponent());
    }

    public boolean isCharacterClone(int index) {
        return index >= getSource().getFirstEdgeCloneIndex();
    }

    public boolean isCharacter(int index) {
        return !isTaxon(index) && !isCharacterClone(index);
    }

    public boolean isTaxon(int index) {
        return index < getSource().numTaxa();
    }

    public int getCloneIndex(int edgeIndex) {
        return edgeIndex + getSource().getFirstEdgeCloneIndex();
    }

    public int getCharIndex(int cloneIndex) {
        return cloneIndex - getSource().getFirstEdgeCloneIndex();
    }


    private boolean isConnected(RoaringBitmap component) {
        return component.equals(taxa);
    }
}
