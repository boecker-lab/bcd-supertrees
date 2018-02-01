package mincut.cutGraphImpl.minCutKargerStein;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com) based on https://gist.github.com/MastaP
 */

import java.util.LinkedHashSet;

public class KargerStein<G extends KargerGraph<G>> {
    public static final double SQRT2 = Math.sqrt(2d);
    private G best;
    private LinkedHashSet<G> cuts;

    private G recursiveContract(G gr) {
        final int n = gr.getNumberOfVertices();
        if (n <= 6) {
            G g1 = contract(gr, 2);
            cuts.add(g1);
            return g1;
        } else {
            final int contractTo = (int) Math.ceil((((double) n) / SQRT2) + 1d);

            G g1 = recursiveContract(contract(gr.clone(), contractTo));
            G g2 = recursiveContract(contract(gr.clone(), contractTo));

            return (g1.getSumOfWeights() < g2.getSumOfWeights()) ? g1 : g2;
        }
    }


    public LinkedHashSet<G> getMinCuts(final G gr, final boolean recursive) {
        if (recursive) {
            cuts = new LinkedHashSet<>();
            best = recursiveContract(gr);
        } else {
            best = null;
            final int n = gr.getNumberOfVertices();
            int iter = (int) Math.ceil(n * n * (Math.log(n) / Math.log(2)));
            cuts = new LinkedHashSet<>(iter);

            for (int i = 0; i < iter; i++) {
                G grc = gr.clone();
                contract(grc, 2);
                cuts.add(grc);
                if (best == null || grc.mincutValue() < best.mincutValue())
                    best = grc;
            }
        }
        return cuts;
    }

    public G getMinCut(final G gr, final boolean recursive) {
        getMinCuts(gr, recursive);
        return best;
    }

    private static <G extends KargerGraph<G>> G contract(G gr, int numOfVerticesLeft) {
        while (gr.getNumberOfVertices() > numOfVerticesLeft) {
            gr.contract();
        }
        return gr;
    }

    public static LinkedHashSet<SimpleGraph> getMinCuts(final int[][] arr, final boolean recursive) {
        SimpleGraph gr = GraphUtils.createGraph(arr);
        return new KargerStein<SimpleGraph>().getMinCuts(gr, recursive);
    }

    public static SimpleGraph getMinCut(final int[][] arr, final boolean recursive) {
        SimpleGraph gr = GraphUtils.createGraph(arr);
        KargerStein<SimpleGraph> k = new KargerStein<>();
        k.getMinCuts(gr, recursive);
        return k.best;
    }

    public static <G extends KargerGraph<G>> G sampleCut(G gr) {
        G grc = gr.clone();
        contract(grc, 2);
        return grc;
    }


}