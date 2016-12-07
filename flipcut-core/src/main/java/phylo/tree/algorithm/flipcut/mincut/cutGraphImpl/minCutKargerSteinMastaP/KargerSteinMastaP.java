package phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 */

/**
 * @author https://gist.github.com/MastaP
 */
import gnu.trove.set.TIntSet;

import java.util.*;

import static phylo.tree.algorithm.flipcut.mincut.cutGraphImpl.minCutKargerSteinMastaP.GraphUtils.getArray;

public class KargerSteinMastaP {

    public int contract(Graph gr ) {
        Random rnd = new Random();

        while( gr.vertices.size() > 2 ) {
            Edge edge = gr.edges.remove( rnd.nextInt( gr.edges.size() ) );
            Vertex v1 = cleanVertex( gr, edge.ends.get( 0 ), edge );
            Vertex v2 = cleanVertex( gr, edge.ends.get( 1 ), edge );
            //contract
            Vertex mergedVertex = new Vertex( v1.lbl );
            mergedVertex.mergedLbls.addAll(v1.mergedLbls);
            mergedVertex.mergedLbls.addAll(v2.mergedLbls);
            redirectEdges( gr, v1, mergedVertex );
            redirectEdges( gr, v2, mergedVertex );
            gr.addVertex( mergedVertex );
        }
        return gr.edges.size();
    }

    public Map<Integer, TIntSet> getMinCut(final int[][] arr){
        Map<Integer, TIntSet> statistics = new LinkedHashMap<>();
        int min = arr.length;

        int iter = arr.length*arr.length;
        for( int i = 0; i < iter; i++ ) {
            Graph gr = GraphUtils.createGraph( arr );
//            GraphUtils.printGraph( gr );
            int currMin = contract( gr );
            min = Math.min(min,currMin);

            min = Math.min( min, currMin );

            statistics.put( currMin, gr.vertices.values().iterator().next().mergedLbls );
        }


        System.out.println( "Min: " + min + " Cutset: " + Arrays.toString(statistics.get(min).toArray()));
        return statistics;
    }

    private Vertex cleanVertex( Graph gr, Vertex v, Edge e ) {
        gr.vertices.remove( v.lbl );
        v.edges.remove( e );
        return v;
    }

    private void redirectEdges( Graph gr, Vertex fromV, Vertex toV ) {
        for ( Iterator<Edge> it = fromV.edges.iterator(); it.hasNext(); ) {
            Edge edge = it.next();
            it.remove();
            if( edge.getOppositeVertex( fromV ) == toV ) {
                //remove self-loop
                toV.edges.remove( edge );
                gr.edges.remove( edge );
            } else {
                edge.replaceVertex( fromV, toV );
                toV.addEdge( edge );
            }
        }
    }
}