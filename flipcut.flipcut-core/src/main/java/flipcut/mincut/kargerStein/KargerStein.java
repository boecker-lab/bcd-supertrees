package flipcut.mincut.kargerStein;


import epos.model.tree.Tree;
import epos.model.tree.TreeNode;
import epos.model.tree.io.Newick;


import java.io.File;
import java.util.*;


public class KargerStein {
    Karger_Graph graphI;
    Karger_Graph graphII;

    public KargerStein(Edge[] edgearray, int vertexCount) { //start algorithm
        graphI = new Karger_Graph(edgearray);
        graphI.dfs_map = new HashMap<Integer, Integer>();
        for (int key : graphI.vertex_map.keySet()) {
            graphI.dfs_map.put(key, 0);
        }
        int start=0;
        for(int i=0;i<graphI.edge_array_global.length;i++){
            if(graphI.edge_array_global[i].weight>0){
                start=graphI.edge_array_global[i].ingoing;
            }
        }

        dfs(graphI, start);
        System.out.println("DFS:" + graphI.dfs_count);
        System.out.println("Vertexcount:"+graphI.vertexCount);
        for(int x: graphI.dfs_map.keySet()){
            if (graphI.dfs_map.get(x)==0){
                System.out.println(x);
            }
        }

        recursive_Contract(graphI, graphI.vertexCount);


    }

    void recursive_Contract(Karger_Graph graphI, int n) {

       /* if(!graphI.ongoing){
            System.out.println("mincut:" + graphI.mincut);
        }*/

        //stop recursion at <6 vertices

        if (graphI.vertexCount < 6 && graphI.ongoing ) {

            graphII = contract(graphI, 2);
            System.out.println(graphII.vertexCount);

            //print mincut value    //THis is not he mincut (sum of cut edges)
            HashSet<Integer> colorset = new HashSet<>();

            for(int i=0;i<graphII.edge_array_global.length;i++){
                if (graphII.edge_array_global[i].weight>0 && !colorset.contains(graphII.edge_array_global[i].color)){
                    graphII.mincut+=graphII.edge_array_global[i].weight;
                    colorset.add(graphII.edge_array_global[i].color);

                   //System.out.println(graphII.edge_array_global[i].outgoing+":"+graphII.edge_array_global[i].ingoing+":"+graphII.edge_array_global[i].weight+":"+graphII.edge_array_global[i].color);
                }
            }

            System.out.println("mincut:" + graphII.mincut); //TODO: give partitions

            //if >6 vertices

        } else if(graphI.ongoing) {
            Karger_Graph temp = new Karger_Graph(graphI);
            for (int i = 0; i < 2; i++) {
                if (i == 0) {
                    graphII = contract(graphI, (int) Math.ceil(n / (Math.sqrt(2) + 1)));
                    recursive_Contract(graphII, (int) Math.ceil(n / (Math.sqrt(2) + 1)));
                } else {
                    graphII = contract(temp, (int) Math.ceil(n / (Math.sqrt(2) + 1)));
                    recursive_Contract(graphII, (int) Math.ceil(n / (Math.sqrt(2) + 1)));
                }
            }
        }

    }

    private Karger_Graph contract(Karger_Graph graph, int goal_vertex_nr) {



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
        */}


        return graph;


    }

    public Karger_Graph merge_same_color_edge(Karger_Graph graph, Edge chosen){ // not used atm

        for (Edge edge : graph.color_map.get(chosen.color)){
                if (edge!=chosen && edge.weight>0){



                    //merge edge
                    int chosen_in=edge.ingoing;
                    int chosen_out=edge.outgoing;
                    int chosen_color=edge.color;

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

 public   Edge chose_Edge(Karger_Graph graph) {
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

        double max_value=0;
        for(int i=0;i< graph.edge_array_global.length;i++){
            if(graph.color_map.get(graph.edge_array_global[i].color).size()!=0) {
                graph.edge_array_global[i].prob = (double) graph.edge_array_global[i].weight / (double) graph.color_map.get(graph.edge_array_global[i].color).size();
                max_value += graph.edge_array_global[i].prob;
            }
            else {
                graph.edge_array_global[i].prob=0;
            }
        }

        double randLong =  (rand.nextDouble()*max_value);

        double currscore=0;

        for(int i=0;i<graph.edge_array_global.length;i++){
            currscore+=graph.edge_array_global[i].prob;
            if(currscore>=randLong)
                return graph.edge_array_global[i];

        }
        return null;

    }

    public static void main(String[] args) {

        KargerStein test;


        //parse trees

        Newick newick = new Newick();
        File file = new File("/home/martin-laptop/trees/SMIDGenOG_Martin/1000.100/smo.1.sourceTrees.tre");
        Tree[] tree_arr = newick.getTreeFromFile(file);
        ArrayList<Edge> edge_list = new ArrayList<Edge>();


        int color_2=0;
        HashSet<String> globallist= new HashSet<>();
        for(int i=0; i<tree_arr.length;i++) {

            for (TreeNode vert : tree_arr[i].vertices()) {


                if (!vert.isLeaf() && vert.isInnerNode() && tree_arr[i].getRoot() != vert) {
                    //System.out.println(vert.getLabel());

                    TreeNode[] leaves = vert.getLeaves();


                    ArrayList<Integer> leavelist = new ArrayList<Integer>();
                    for (TreeNode leave : leaves) {


                        leavelist.add(Integer.parseInt(leave.getLabel().substring(1)));
                        globallist.add(leave.getLabel());
                    }

                    Collections.sort(leavelist);

                    for (int x = 0; x < leavelist.size() - 1; x++) {
                        for (int j = x + 1; j < leavelist.size(); j++) {

                            Edge new_edge = new Edge(leavelist.get(x), leavelist.get(j), Integer.parseInt(vert.getLabel()), color_2);
                           // System.out.println(new_edge.outgoing + ":" + new_edge.ingoing + ":" + new_edge.weight + ":" + new_edge.color);
                            edge_list.add(new_edge);
                        }
                    }

                }


                color_2 += 1;
            }
        }
        ArrayList<String> globallist2 = new ArrayList(globallist);
        Collections.sort(globallist2);

        Edge[] edgearr = new Edge[edge_list.size()];
        edgearr=edge_list.toArray(edgearr);









       // Edge[] edgearr = {new Edge(1,4,4,1), new Edge(1,3,4,2),new Edge(2, 4, 4, 3), new Edge(2, 3, 4, 1)};

        test = new KargerStein(edgearr, 100);

//creates random connected Graph with n vertices and n-1 edges

      /*  int nr_vertices = 1000;
        Random ran = new Random();
        Edge[] edgeset = new Edge[nr_vertices - 1];
        HashMap<Integer, LinkedList<Integer>> vertex_need_work = new HashMap<>();
        LinkedList<Integer> lista = new LinkedList<Integer>();
        LinkedList<Integer> listb = new LinkedList<Integer>();
        vertex_need_work.put(1, lista);
        vertex_need_work.put(0, listb);
        for (int i = 0; i < nr_vertices; i++) {
            vertex_need_work.get(1).add(i);

        }

        int ran_a = ran.nextInt(vertex_need_work.get(1).size());
        int start_a = vertex_need_work.get(1).get(ran_a);
        vertex_need_work.get(1).remove(ran_a);
        vertex_need_work.get(0).add(start_a);

        int ran_b = ran.nextInt(vertex_need_work.get(1).size());
        int start_b = vertex_need_work.get(1).get(ran_b);
        vertex_need_work.get(1).remove(ran_b);
        vertex_need_work.get(0).add(start_b);

        edgeset[0] = new Edge(Math.min(start_a, start_b), Math.max(start_a, start_b), ran.nextInt(100) + 1, 0);


        for (int i = 1; i < nr_vertices - 1; i++) {
           // System.out.println(ran.nextInt(vertex_need_work.get(1).size()));

            int chosen_new_random = ran.nextInt(vertex_need_work.get(1).size());
            int chosen_new_vertex = vertex_need_work.get(1).get(chosen_new_random);

            vertex_need_work.get(1).remove(chosen_new_random);

            int chosen_old_random = ran.nextInt(vertex_need_work.get(0).size());
            int chosen_old = vertex_need_work.get(0).get(chosen_old_random);
            vertex_need_work.get(0).add(chosen_new_vertex);
            int weight = ran.nextInt(100) + 1;
            int color = ran.nextInt(14000);
            edgeset[i] = new Edge(Math.min(chosen_old, chosen_new_vertex), Math.max(chosen_new_vertex, chosen_old), weight, i);


        }

        test = new KargerStein(edgeset, nr_vertices);*/


    }

    public void dfs(Karger_Graph graph, int vertex_curr){




        if( graph.dfs_map.get(vertex_curr)==0) {
            graph.dfs_map.remove(vertex_curr);
            graph.dfs_map.put(vertex_curr,1);
            graph.dfs_count+=1;

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


class Edge {
    int ingoing;
    int outgoing;
    int weight;
    int color;
    double prob;


    public Edge(int out, int in, int weight, int color) {
        ingoing = in;
        outgoing = out;
        this.weight = weight;
        this.color = color;
    }


}

class Karger_Graph {
    Edge[] edge_array_global;
    int[] kumu;
    int vertexCount;
    int edgeCount;
    int dfs_count=0;
    int mincut=0;
    boolean ongoing=true;
    HashMap<Integer, Integer> dfs_map;

    HashMap<Integer, HashSet<Edge>> vertex_map = new HashMap<>();
    HashMap<Integer, HashSet<Edge>> color_map = new HashMap<>();
    int weight_kumu = 0;
    HashMap<Integer, Integer> kumu_map;

    public Karger_Graph(Karger_Graph graph) {   //TODO; Fix lazy c&p
        this.edge_array_global = new Edge[graph.edge_array_global.length];
        for (int i = 0; i < edge_array_global.length; i++) {
            this.edge_array_global[i] = new Edge(graph.edge_array_global[i].outgoing, graph.edge_array_global[i].ingoing, graph.edge_array_global[i].weight, graph.edge_array_global[i].color);
        }
        this.kumu = graph.kumu.clone();
        this.vertexCount = graph.vertexCount;
        this.mincut=graph.mincut;
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

    public Karger_Graph(Edge[] edgearray) {
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
        vertexCount=vertex_map.size();



    }
}


