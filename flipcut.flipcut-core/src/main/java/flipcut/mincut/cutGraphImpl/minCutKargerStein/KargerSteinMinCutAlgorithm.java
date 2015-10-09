package flipcut.mincut.cutGraphImpl.minCutKargerStein;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;


public class KargerSteinMinCutAlgorithm {
    private static final double SQRT2 = Math.sqrt(2) + 1;

    public KargerSteinMinCutAlgorithm(Edge[] edgearray) { //start algorithm //todo no edge here. Edge class should be a package internal data structure.
        KargerGraph inititalGraph = new KargerGraph(edgearray);
        inititalGraph.dfs_map = new HashMap<Integer, Integer>();
        for (int key : inititalGraph.vertex_map.keySet()) {
            inititalGraph.dfs_map.put(key, 0);
        }
        int start = 0;
        for (int i = 0; i < inititalGraph.edge_array_global.length; i++) {
            if (inititalGraph.edge_array_global[i].weight > 0) {
                start = inititalGraph.edge_array_global[i].ingoing;
            }
        }

        dfs(inititalGraph, start);
        System.out.println("DFS:" + inititalGraph.dfs_count);
        System.out.println("Vertexcount:" + inititalGraph.vertexCount);
        for (int x : inititalGraph.dfs_map.keySet()) {
            if (inititalGraph.dfs_map.get(x) == 0) {
                System.out.println(x);
            }
        }

        recursive_Contract(inititalGraph, inititalGraph.vertexCount);


    }

    void recursive_Contract(KargerGraph currentGraph, final int n) {

       /* if(!graphI.ongoing){
            System.out.println("mincut:" + graphI.mincut);
        }*/

        //stop recursion at <6 vertices

        KargerGraph contractedGraph = null;
        if (currentGraph.vertexCount < 6 && currentGraph.ongoing) {

            contractedGraph = contract(currentGraph, 2);
            System.out.println(contractedGraph.vertexCount);

            //print mincut value    //THis is not he mincut (sum of cut edges)
            HashSet<Integer> colorset = new HashSet<>();

            for (int i = 0; i < contractedGraph.edge_array_global.length; i++) {
                if (contractedGraph.edge_array_global[i].weight > 0 && !colorset.contains(contractedGraph.edge_array_global[i].color)) {
                    contractedGraph.mincut += contractedGraph.edge_array_global[i].weight;
                    colorset.add(contractedGraph.edge_array_global[i].color);

                    //System.out.println(graphII.edge_array_global[i].outgoing+":"+graphII.edge_array_global[i].ingoing+":"+graphII.edge_array_global[i].weight+":"+graphII.edge_array_global[i].color);
                }
            }

            System.out.println("mincut:" + contractedGraph.mincut); //TODO: give partitions

            //if >6 vertices
        } else if (currentGraph.ongoing) {
            int repeats = (int) Math.ceil(n / SQRT2);
            contractedGraph = contract(new KargerGraph(currentGraph), repeats);
            recursive_Contract(contractedGraph, repeats);
            contractedGraph = contract(currentGraph, repeats);
            recursive_Contract(contractedGraph, repeats);

            /*KargerGraph temp = new KargerGraph(currentGraph);
            for (int i = 0; i < 2; i++) {
                if (i == 0) {
                    contractedGraph = contract(currentGraph, (int) Math.ceil(n / (Math.sqrt(2) + 1)));
                    recursive_Contract(contractedGraph, (int) Math.ceil(n / (Math.sqrt(2) + 1)));
                } else {
                    contractedGraph = contract(temp, (int) Math.ceil(n / (Math.sqrt(2) + 1)));
                    recursive_Contract(contractedGraph, (int) Math.ceil(n / (Math.sqrt(2) + 1)));
                }
            }*/
        }

    }

    private KargerGraph contract(KargerGraph graph, int goal_vertex_nr) {

        while (graph.vertexCount >= goal_vertex_nr) {

            Edge chosen = chose_Edge(graph);

            int chosenin = chosen.ingoing;
            int chosenout = chosen.outgoing;

            System.out.println("Merging " + chosenout + " in " + chosenin);

            for (Edge x : graph.vertex_map.get(chosenin)) {

                if ((x.outgoing == chosenout && x.ingoing == chosenin)) {//||(x.outgoing==chosenin&&x.ingoing==chosenout)){
                    x.weight = 0;
                    graph.vertex_map.remove(x);  //remove 0 elements
                    graph.color_map.get(x.color).remove(x); //this should be necessary for probability calculation

                    graph.edgeCount--;
                }
            }

            graph.color_map.get(chosen.color).remove(chosen);


            //update vertex map
            for (Edge x : graph.vertex_map.get(chosenout)) {

                if (x != chosen && x.weight > 0) {
                    if (x.ingoing == chosenout) {
                        x.ingoing = chosenin;

                    } else {
                        if (x.ingoing > chosenin)
                            x.outgoing = chosenin;
                        else {
                            x.outgoing = x.ingoing;
                            x.ingoing = chosenin;
                        }


                    }
                    graph.vertex_map.get(chosenin).add(x);


                }

            }


            graph.vertexCount -= 1;

            //graph=merge_same_color_edge(graph,chosen);
            //check if edges got deleted
            // boolean deleted_colored_extra=false;
            //DFS search, not needed anymore
           /* if(deleted_colored_extra==true) {
                graph.dfs_map = new HashMap<Integer, Integer>();
                for (int key : graph.vertex_map.keySet()) {
                    graph.dfs_map.put(key, 0);
                }
                int start=0;
                for(int i=0;i<graph.edge_array_global.length;i++){
                    if(graph.edge_array_global[i].weight>0){
                       start=graph.edge_array_global[i].ingoing;
                    }
                }
                System.out.println(graph.vertex_map.keySet().size());
                dfs(graph, start);
                System.out.println("DFS:" + graph.dfs_count);
                System.out.println("Vertexcount:"+graph.vertexCount);
                if (graph.dfs_count != graph.vertexCount) {
                    System.out.println("NOT CONNECTED");
                    ;
                    graph.ongoing=false;
                    return  graph;
                }
                graph.dfs_count = 0;
            }

            if (graph.vertexCount == goal_vertex_nr)
                return graph;
        */
        }


        return graph;


    }

    public KargerGraph merge_same_color_edge(KargerGraph graph, Edge chosen) { // not used atm

        for (Edge edge : graph.color_map.get(chosen.color)) {
            if (edge != chosen && edge.weight > 0) {


                //merge edge
                int chosen_in = edge.ingoing;
                int chosen_out = edge.outgoing;
                int chosen_color = edge.color;

                for (Edge x : graph.vertex_map.get(chosen_in)) {

                    if ((x.outgoing == chosen_out && x.ingoing == chosen_in)) {

                        x.weight = 0;

                        graph.edgeCount--;

                    }
                }


                //System.out.println("chosen edge: "+chosen.ingoing+":"+chosen.outgoing+":"+chosen.color);
                //System.out.println(chosen_in+":"+chosen_out+":"+chosen_color);


                //update vertex map
                for (Edge x : graph.vertex_map.get(chosen_out)) {

                    if (x != chosen && x.weight > 0) {
                        if (x.ingoing == chosen_out) {
                            x.ingoing = chosen_in;

                        } else {
                            if (x.ingoing > chosen_in)
                                x.outgoing = chosen_in;
                            else {
                                x.outgoing = x.ingoing;
                                x.ingoing = chosen_in;
                            }


                        }
                        graph.vertex_map.get(chosen_in).add(x);


                    }

                }
                graph.vertexCount -= 1;


            }


        }

        return graph;

    }

    public Edge chose_Edge(KargerGraph graph) {
        Random rand = new Random();


      /*  graph.kumu_map = new HashMap();
        graph.weight_kumu = 0;

        for (int i = 0; i < graph.edge_array_global.length; i++) {
            if (graph.edge_array_global[i].weight != 0) {
                graph.weight_kumu += graph.edge_array_global[i].weight;
                graph.kumu[i] = graph.weight_kumu;
                for (int j = graph.weight_kumu - graph.edge_array_global[i].weight; j <= graph.kumu[i]; j++) {
                    if (i != graph.edge_array_global.length - 1 || j != graph.kumu[i])
                        graph.kumu_map.put(j + 1, i);
                }
            }
        }


        int randInt = 0;
        //try {

            randInt = rand.nextInt(graph.weight_kumu) + 1;
        // catch (Exception e) {
          //  System.out.println(graph.weight_kumu);
            //for (int i = 0; i < graph.edge_array_global.length; i++)
              //  System.out.print(graph.edge_array_global[i].weight);


        //}

        Edge chosen = graph.edge_array_global[graph.kumu_map.get(randInt)];

        return chosen;*/

        double max_value = 0;
        for (int i = 0; i < graph.edge_array_global.length; i++) {
            if (graph.color_map.get(graph.edge_array_global[i].color).size() != 0) {
                graph.edge_array_global[i].prob = (double) graph.edge_array_global[i].weight / (double) graph.color_map.get(graph.edge_array_global[i].color).size();
                max_value += graph.edge_array_global[i].prob;
            } else {
                graph.edge_array_global[i].prob = 0;
            }
        }

        double randLong = (rand.nextDouble() * max_value);

        double currscore = 0;

        for (int i = 0; i < graph.edge_array_global.length; i++) {
            currscore += graph.edge_array_global[i].prob;
            if (currscore >= randLong)
                return graph.edge_array_global[i];

        }
        return null;

    }

    public void dfs(KargerGraph graph, int vertex_curr) {
        if (graph.dfs_map.get(vertex_curr) == 0) {
            graph.dfs_map.remove(vertex_curr);
            graph.dfs_map.put(vertex_curr, 1);
            graph.dfs_count += 1;

            for (int i = 0; i < graph.edge_array_global.length; i++) {

                if ((graph.edge_array_global[i].outgoing == vertex_curr && graph.edge_array_global[i].weight != 0 && graph.dfs_map.get(graph.edge_array_global[i].ingoing) == 0)) {
                    dfs(graph, graph.edge_array_global[i].ingoing);
                } else if ((graph.edge_array_global[i].ingoing == vertex_curr && graph.edge_array_global[i].weight != 0 && graph.dfs_map.get(graph.edge_array_global[i].outgoing) == 0)) {
                    dfs(graph, graph.edge_array_global[i].outgoing);
                }
            }

        }
    }

}







