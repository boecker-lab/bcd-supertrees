package phylo.tree.algorithm.flipcut.bcdGraph;

import org.roaringbitmap.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class CompressedBCDGraph {
    protected final RoaringBitmap taxa;
    final RoaringBitmap characters;

    protected CompressedBCDGraph(RoaringBitmap taxa, RoaringBitmap characters) {
        this.taxa = taxa;
        this.characters = characters;
    }


    public abstract LinkedList<RoaringBitmap> getHyperEdgesAsList();

    public abstract Iterable<RoaringBitmap> hyperEdges();

    public abstract Iterable<RoaringBitmap> imaginaryHyperEdges();

    public abstract CompressedBCDSourceGraph getSource();


    public int numTaxa() {
        return taxa.getCardinality();
    }

    public int numCharacter() {
        return characters.getCardinality();
    }

    public RoaringBitmap getConnectedComponent() {
        LinkedList<RoaringBitmap> edges = getHyperEdgesAsList();
        RoaringBitmap component = edges.poll().clone();
        boolean changed = true;
        while (changed) {
            changed = false;
            Iterator<RoaringBitmap> it = edges.iterator();
            while (it.hasNext()) {
                RoaringBitmap next = it.next();
                if (RoaringBitmap.intersects(component, next)) {
                    component.or(next);
                    it.remove();
                    changed = true;
                }
            }
        }
        return component;
    }

    public void deleteSemiUniversals() {
        RoaringBitmap toDelete = new RoaringBitmap();
        characters.forEach((IntConsumer) i -> {
            if (!RoaringBitmap.intersects(getSource().getImaginaryHyperEdge(i), taxa)) {
                toDelete.add(i);
            }
        });
        deleteCharacters(toDelete);
    }

    public void deleteCharacters(RoaringBitmap toDelete) {
        characters.andNot(toDelete);
    }

    public List<CompressedBCDSubGraph> split() {
        List<CompressedBCDSubGraph> comps = new ArrayList<>();
        split(comps);
        return comps;
    }

    protected void split(final List<CompressedBCDSubGraph> graphs) {
        RoaringBitmap connectedTaxa = getConnectedComponent();
        CompressedBCDSubGraph gCurrent = new CompressedBCDSubGraph(getSource(), connectedTaxa, getCharacterForSubSetOfTaxa(connectedTaxa));
        graphs.add(gCurrent);
        RoaringBitmap reverseTaxa = RoaringBitmap.xor(connectedTaxa, taxa);
        if (reverseTaxa.getCardinality() > 0) {
            CompressedBCDSubGraph gRest = new CompressedBCDSubGraph(getSource(), connectedTaxa, getCharacterForSubSetOfTaxa(connectedTaxa));
            gRest.split(graphs);
        }
    }

    protected RoaringBitmap getCharacterForSubSetOfTaxa(RoaringBitmap taxa) {
        RoaringBitmap subCharacters = new RoaringBitmap();
        characters.forEach((IntConsumer) i -> {
            if (RoaringBitmap.intersects(getSource().getHyperEdge(i), taxa)) {
                subCharacters.add(i);
            }
        });
        return subCharacters;
    }


}
