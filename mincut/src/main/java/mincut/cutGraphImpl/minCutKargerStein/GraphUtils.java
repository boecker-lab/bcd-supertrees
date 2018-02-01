package mincut.cutGraphImpl.minCutKargerStein;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the flipcut
 * 07.12.16.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class GraphUtils {

    public static int[][] getArray(String relPath) {

        Map<Integer, List<Integer>> vertices = new LinkedHashMap<Integer, List<Integer>>();

        FileReader fr;
        try {
            fr = new FileReader(relPath);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.trim().split("(\\s)+");
                List<Integer> adjList = new ArrayList<Integer>();
                for (int i = 1; i < split.length; i++) {
                    adjList.add(Integer.parseInt(split[i]) - 1);
                }
                vertices.put(Integer.parseInt(split[0]) - 1, adjList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        int[][] array = new int[vertices.size()][];
        for (Map.Entry<Integer, List<Integer>> entry : vertices.entrySet()) {
            List<Integer> adjList = entry.getValue();
            int[] adj = new int[adjList.size()];
            for (int i = 0; i < adj.length; i++) {
                adj[i] = adjList.get(i);
            }
            array[entry.getKey()] = adj;
        }
        return array;
    }

    public static SimpleGraph createGraph(final int[][] array) {
        SimpleGraph gr = new SimpleGraph();
        for (int i = 0; i < array.length; i++) {
            Vertex v = gr.getVertex(i);
            if (v == null) {
                v = new Vertex(i);
                gr.addVertex(v);
            }
            for (int edgeTo : array[i]) {
                Vertex v2 = gr.getVertex(edgeTo);
                if (v2 == null) {
                    v2 = new Vertex(edgeTo);
                    gr.addVertex(v2);
                }
                if ((v2.getEdgeTo(v)) == null) {
                    gr.addEdge(v,v2);
                }
            }
        }
        return gr;
    }

    public static void printGraph(SimpleGraph gr) {
        System.out.println("Printing graph");
        for (Object o : gr.vertices.values()) {
            Vertex v = (Vertex) o;
            System.out.print(v.lbl + ":");
            for (Edge edge : v.edges) {
                System.out.print(" " + edge.getOppositeVertex(v).lbl);
            }
            System.out.println();
        }
    }

    //Adj format to visualize in
    //http://www.cs.rpi.edu/research/groups/pb/graphdraw/headpage.html
    public static void toAdjFormat(int[][] arr) {
        System.out.println(arr.length);
        for (int[] adj : arr) {
            System.out.print(adj.length);
            for (int i : adj) {
                System.out.print(" " + i);
            }
            System.out.println();
        }
    }
}
