package flipcut.mincut.cutGraphImpl.minCutKargerStein;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by fleisch on 09.10.15.
 */
class KargerGraph {
    Edge[] edge_array_global;
    int[] kumu;
    int vertexCount;
    int edgeCount;
    int dfs_count = 0;
    int mincut = 0;
    boolean ongoing = true;
    HashMap<Integer, Integer> dfs_map;

    HashMap<Integer, HashSet<Edge>> vertex_map = new HashMap<>();
    HashMap<Integer, HashSet<Edge>> color_map = new HashMap<>();
    int weight_kumu = 0;
    HashMap<Integer, Integer> kumu_map;

    public KargerGraph(KargerGraph graph) {   //TODO; Fix lazy c&p
        this.edge_array_global = new Edge[graph.edge_array_global.length];
        for (int i = 0; i < edge_array_global.length; i++) {
            this.edge_array_global[i] = new Edge(graph.edge_array_global[i].outgoing, graph.edge_array_global[i].ingoing, graph.edge_array_global[i].weight, graph.edge_array_global[i].color);
        }
        this.kumu = graph.kumu.clone();
        this.vertexCount = graph.vertexCount;
        this.mincut = graph.mincut;
        this.edgeCount = graph.edgeCount;
        for (Edge x : edge_array_global) {
            if (vertex_map.containsKey(x.ingoing))
                vertex_map.get(x.ingoing).add(x);
            if (vertex_map.containsKey(x.outgoing))
                vertex_map.get(x.outgoing).add(x);
            if (!vertex_map.containsKey(x.ingoing)) {
                vertex_map.put(x.ingoing, new HashSet<Edge>());
                vertex_map.get(x.ingoing).add(x);
            }
            if (!vertex_map.containsKey(x.outgoing)) {
                vertex_map.put(x.outgoing, new HashSet<Edge>());
                vertex_map.get(x.outgoing).add(x);
            }


            if (color_map.containsKey(x.color)) {
                color_map.get(x.color).add(x);
            } else {
                color_map.put(x.color, new HashSet<Edge>());
                color_map.get(x.color).add(x);
            }
        }
    }

    public KargerGraph(Edge[] edgearray) {
        edge_array_global = edgearray;
        kumu = new int[edge_array_global.length + 1];

        edgeCount = edgearray.length;


        for (Edge x : edge_array_global) {
            if (vertex_map.containsKey(x.ingoing))
                vertex_map.get(x.ingoing).add(x);
            if (vertex_map.containsKey(x.outgoing))
                vertex_map.get(x.outgoing).add(x);
            if (!vertex_map.containsKey(x.ingoing)) {
                vertex_map.put(x.ingoing, new HashSet<Edge>());
                vertex_map.get(x.ingoing).add(x);
            }
            if (!vertex_map.containsKey(x.outgoing)) {
                vertex_map.put(x.outgoing, new HashSet<Edge>());
                vertex_map.get(x.outgoing).add(x);
            }


            if (color_map.containsKey(x.color)) {
                color_map.get(x.color).add(x);
            } else {
                color_map.put(x.color, new HashSet<Edge>());
                color_map.get(x.color).add(x);


                //TODO: Lösche Kanten weight=0 aus colormap
            }
        }
        vertexCount = vertex_map.size();
    }

    //internal edge data structure

}
