package mincut.cutGraphImpl.minCutKargerStein;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com) based on https://gist.github.com/MastaP
 */

import gnu.trove.set.TIntSet;
import mincut.cutGraphAPI.bipartition.HashableCut;

import java.util.*;
import java.util.stream.Collectors;

public class KargerStein<G extends KargerGraph<G, S>, S> {
    public static final double SQRT2 = Math.sqrt(2d);
    private TreeMap<Double, Set<HashableCut<S>>> cutsSorted;
    private int sumOfCuts;


    private int maxCuts = Integer.MAX_VALUE;

    private G recursiveContract(G gr) {
        assert gr.isCutted();
        final int n = gr.getNumberOfVertices();
        if (n <= 6) {
            contract(gr, 2);
            addCut(gr);
            return gr;
        } else {
            final int contractTo = (int) Math.ceil((((double) n) / SQRT2) + 1d);

            G grClone = contractAndKeep(gr, contractTo);
            contract(grClone, contractTo);

            G g1 = recursiveContract(gr);
            G g2 = recursiveContract(grClone);


            return (g1.getSumOfWeights() < g2.getSumOfWeights()) ? g1 : g2;
        }
    }

    public void setMaxCutNumber(int maxCuts) {
        this.maxCuts = maxCuts;
    }

    public int getMaxCutNumber() {
        return maxCuts;
    }

    public List<HashableCut<S>> getMinCuts(final G gr, final boolean recursive) {
        final int n = gr.getNumberOfVertices();
        cutsSorted = new TreeMap<>();

        if (recursive) {
            final int iter = (int) ((Math.log(n) / Math.log(2)) * (Math.log(n) / Math.log(2)));
            for (int i = 0; i < iter; i++) {
                recursiveContract(gr.clone());
            }
        } else {
            final int iter = (int) (n * n * (Math.log(n) / Math.log(2)));
            for (int i = 0; i < iter; i++) {
                G clone = gr.clone();
                contract(clone, 2);
                addCut(clone);
            }
        }

        return cutsSorted.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    private void addCut(G graph) {
        final double weight = graph.getSumOfWeights();

        if (cutsSorted.size() < maxCuts || cutsSorted.lastKey() > weight) {
            final HashableCut<S> cut = graph.asCut();
            final Set<HashableCut<S>> cuts = cutsSorted.computeIfAbsent(weight, k -> new HashSet<>());
            if (cuts.add(cut))
                sumOfCuts++;

            if (sumOfCuts > maxCuts) {
                Map.Entry<Double, Set<HashableCut<S>>> last = cutsSorted.lastEntry();
                Set<HashableCut<S>> lastValue = last.getValue();
                if (lastValue.remove(lastValue.iterator().next()))
                    sumOfCuts--;
                if (lastValue.isEmpty()) {
                    cutsSorted.remove(last.getKey());
                }
            }
        }

            /*while (sumOfCuts > maxCuts) {
                Map.Entry<Double, Set<G>> last = cutsSorted.lastEntry();
                Set<G> lastValue = last.getValue();
                while(sumOfCuts > maxCuts){
                    if (lastValue.remove(lastValue.iterator().next()))
                        sumOfCuts --;
                    if (lastValue.isEmpty()) {
                        cutsSorted.remove(last.getKey());
                        break;
                    }
                }
            }*/

    }


    public HashableCut<S> getMinCut(final G gr, final boolean recursive) {
        setMaxCutNumber(1);
        return getMinCuts(gr, recursive).get(0);
    }

    private static <G extends KargerGraph<G, S>, S> G contractAndKeep(G gr, int numOfVerticesLeft) {
        assert !gr.isCutted();
        G keep = gr;
        if (gr.getNumberOfVertices() > numOfVerticesLeft) {
            keep = gr.contractAndKeep();
            contract(gr, numOfVerticesLeft);
        }
        return keep;
    }

    private static <G extends KargerGraph<G, S>, S> void contract(G gr, int numOfVerticesLeft) {
        assert !gr.isCutted();
        while (gr.getNumberOfVertices() > numOfVerticesLeft) {
            gr.contract();
        }
    }

    public static List<HashableCut<TIntSet>> getMinCuts(final int[][] arr, final boolean recursive) {
        SimpleGraph gr = GraphUtils.createGraph(arr);
        return new KargerStein<SimpleGraph, TIntSet>().getMinCuts(gr, recursive);
    }

    public static HashableCut<TIntSet> getMinCut(final int[][] arr, final boolean recursive) {
        SimpleGraph gr = GraphUtils.createGraph(arr);
        KargerStein<SimpleGraph, TIntSet> karger = new KargerStein<>();
        return karger.getMinCut(gr, recursive);
    }

    public static <G extends KargerGraph<G, S>, S> G sampleCut(G gr) {
        G grc = gr.clone();
        contract(grc, 2);
        return grc;
    }


}