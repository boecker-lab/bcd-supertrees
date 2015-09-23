/*
 * Epos Phylogeny Framework
 * Copyright (C) 2009.  University of Jena
 *
 * This file is part of Epos.
 *
 * Epos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Epos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Epos.  If not, see <http://www.gnu.org/licenses/>;.
 */

package flipcut.mincut.goldberg_tarjan;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * The actual implementation the the push-relabel maxflow min cut method based on the C
 * implementation from http://www.avglab.com/andrew/soft.html.
 * <p>
 * See:
 * <pre>
  Goldberg and Tarjan, "A New Approach to the Maximum Flow Problem,"
  J. ACM Vol. 35, 921--940, 1988
 </pre>and
 * <pre>
  Cherkassky and Goldberg, "On Implementing Push-Relabel Method for the
  Maximum Flow Problem," Proc. IPCO-4, 157--171, 1995.</pre>
 * <br>
 * We use the same data structures here, but the structure is not that obvious, so
 * please use {@link flipcut.mincut.goldberg_tarjan.GoldbergTarjanCutGraph} as access point and
 * to call functions in this class.
 *
 *
 * @author Thasso Griebel (thasso.griebel@gmail.com)
 */
/*
This class is not cleaned up ! This is more or less a translation from the C code, so don't expect too much ;)
 */
class CutGraphImpl{
    //#define MAXLONG 1073741824
    private static final long MAXLONG = Long.MAX_VALUE - 1;

    private static final float GLOB_UPDT_FREQ = 0.5f;
    private static final int ALPHA = 6;
    private static final int BETA = 12;
    private static final int WHITE = 0;
    private static final int GREY = 1;
    private static final int BLACK = 2;

    private static final boolean FORWARD_SEARCH = true;

/* global variables */

    long n;                    /* number of nodes */
    long m;                    /* number of arcs */
    long nm;                   /* n + ALPHA * m */
    Node[] nodes;               /* array of nodes */
    Bucket[] buckets;             /* array of buckets */
    Node source;              /* source node pointer */
    Node sink;                /* sink node pointer */
//node   **queue;              /* queue for BFS */
//node   **qHead, **qTail, **qLast;     /* queue pointers */
    long dMax;                 /* maximum label */
    long aMax;                 /* maximum actie node label */
    long aMin;                 /* minimum active node label */
    long flow;                 /* flow value */
    long pushCnt = 0;           /* number of pushes */
    long relabelCnt = 0;       /* number of relabels */
    long updateCnt = 0;       /* number of updates */
    long gapCnt = 0;           /* number of gaps */
    long gNodeCnt = 0;           /* number of nodes after gap */
    float t, t2;                 /* for saving times */
    long workSinceUpdate = 0;      /* the number of arc scans since last update */
    float globUpdtFreq;          /* global update frequency */

    long i_dist;

    private int createdNodes = 0;

    public CutGraphImpl(int nodes, int edges) {
        this.n = nodes;
        this.m = edges;
        this.nodes = new Node[nodes];
    }

    public void addEdge(Node ns, Node nt, long cap) {
        ns.addArc(nt, cap);
    }


    public Node createNode(Object name, int edges) {
        nodes[createdNodes] = new Node(edges, name);
        nodes[createdNodes].bucketIndex = createdNodes + 1;
        createdNodes++;
        return nodes[createdNodes - 1];
    }

    private void aAdd(Bucket l, Node i) {
        i.bNext = l.firstActive;
        l.firstActive = i;
        i_dist = i.d;
        if (i_dist < aMin)
            aMin = i_dist;
        if (i_dist > aMax)
            aMax = i_dist;
        if (dMax < aMax)
            dMax = aMax;
    }

    private void aRemove(Bucket l, Node i) {
        l.firstActive = i.bNext;
    }

    private void iAdd(Bucket l, Node i) {
        Node i_next = l.firstInactive;
        i.bNext = i_next;
        i.bPrev = null; // was sentinal node
        if (i_next != null)
            i_next.bPrev = i;
        l.firstInactive = i;
    }

    private void iDelete(Bucket l, Node i) {
        Node i_next = i.bNext;
        if (l.firstInactive == i) {
            l.firstInactive = i_next;
            if (i_next != null)
                i_next.bPrev = null; // was sentinal node
        } else {
            Node i_prev = i.bPrev;
            i_prev.bNext = i_next;
            if (i_next != null)
                i_next.bPrev = i_prev;
        }
    }


    int allocDS() {

        nm = ALPHA * n + m;
        /*
        queue = (node**) calloc ( n, sizeof (node*) );
        if ( queue == NULL ) return ( 1 );
        qLast = queue + n - 1;
        qInit();
        */
        //buckets = (bucket*) calloc ( n+2, sizeof (bucket) );        
        if(buckets == null){
            buckets = new Bucket[(int) (n + 2)];
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = new Bucket();
                buckets[i].index = i;
            }
        }else{
            for (int i = 0; i < buckets.length; i++) {
                buckets[i].index = i;
                buckets[i].firstActive = null;
                buckets[i].firstInactive = null;
            }

        }



//  sentinelNode = nodes + n;
//  sentinelNode->first = arcs + 2*m;

        return (0);

    } /* end of allocate */


    void init()

    {
        int overflowDetected;
        long delta;


        // initialize excesses
        for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            node.excess = 0;
            for (Arc arc : node.arcs) {
                arc.resCap = arc.cap;
            }
        }

        /* skip -- all point to null by default
        for (l = buckets; l <= buckets + n-1; l++) {
          l -> firstActive   = sentinelNode;
          l -> firstInactive  = sentinelNode;
        }
        */

        overflowDetected = 0;
        if (overflowDetected == 1) {
            source.excess = MAXLONG;
        } else {
            source.excess = 0;
            for (Arc a : source.arcs) {
                if (a.head != source) {
                    pushCnt++;
                    delta = a.resCap;
                    a.resCap -= delta;
                    (a.rev).resCap += delta;
                    a.head.excess += delta;
                }
            }
        }


        /*  setup labels and buckets */
        //l = buckets + 1;
        Bucket l = buckets[1];

        aMax = 0;
        aMin = n;
        for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            if (node == sink) {
                node.d = 0;
                iAdd(buckets[0], node);
                continue;
            }
            if ((node == source) && (overflowDetected == 0)) {
                node.d = n;
            } else {
                node.d = 1;
            }
            if (node.excess > 0) {
                /* put into active list */
                aAdd(l, node);
            } else { /* i -> excess == 0 */
                /* put into inactive list */
                if (node.d < n)
                    iAdd(l, node);
            }
        }
        dMax = 1;

        //  dMax = n-1;
        //  flow = 0.0;

    } /* end of init */


    /* global update via backward breadth first search from the sink */

    void globalUpdate()

    {
//  node  *i, *j;       /* node pointers */
//  arc   *a;           /* current arc pointers  */
//  bucket *l, *jL;          /* bucket */
        int curDist, jD;
        int state;


        updateCnt++;

        /* initialization */

        for (Node i : nodes) {
            i.d = n;
        }


        sink.d = 0;

        //for (l = buckets; l <= buckets + dMax; l++) {
        for (int l = 0; l <= dMax; l++) {
            buckets[l].firstActive = null; // was sentinelNode
            buckets[l].firstInactive = null; // was sentinelNode
        }

        dMax = aMax = 0;
        aMin = n;

        /* breadth first search */

        // add sink to bucket zero

        iAdd(buckets[0], sink);
        for (curDist = 0; ; curDist++) {

            state = 0;
            Bucket l = buckets[curDist];
            jD = curDist + 1; // bucket index
            //jL = l + 1;
            Bucket jL = buckets[curDist + 1];
            /*
            jL -> firstActive   = sentinelNode;
            jL -> firstInactive  = sentinelNode;
            */

            if ((l.firstActive == null) &&
                    (l.firstInactive == null))
                break;

            Node i = null;
            while (true) {

                switch (state) {
                    case 0:
                        i = l.firstInactive;
                        state = 1;
                        break;
                    case 1:
                        i = i.bNext;
                        break;
                    case 2:
                        i = l.firstActive;
                        state = 3;
                        break;
                    case 3:
                        i = i.bNext;
                        break;
                    default:
                        assert (false);
                        break;
                }

                if (i == null) { // was sentinelNode
                    if (state == 1) {
                        state = 2;
                        continue;
                    } else {
                        assert (state == 3);
                        break;
                    }
                }

                /* scanning arcs incident to node i */
                for (Arc a : i.arcs) {
                    if (a.rev.resCap > 0) {
                        Node j = a.head;
                        if (j.d == n) {
                            j.d = jD;

                            j.current = 0;

                            if (jD > dMax) dMax = jD;

                            if (j.excess > 0) {
                                /* put into active list */
                                aAdd(jL, j);
                            } else {
                                /* put into inactive list */
                                iAdd(jL, j);
                            }
                        }
                    }
                } /* node i is scanned */
            }
        }

    } /* end of global update */


/* second stage -- preflow to flow */

    void stageTwo()
/*
   do dsf in the reverse flow graph from nodes with excess
   cancel cycles if found
   return excess flow in topological order
*/

/*
   i->d is used for dfs labels
   i->bNext is used for topological order list
   buckets[i-nodes]->firstActive is used for DSF tree
*/

    {
        //node *i, *j, *tos, *bos, *restart, *r;
        //arc *a;
        long delta;

        /* deal with self-loops */
        for (Node i : nodes) {
            for (Arc a : i.arcs) {
                if (a.head == i) {
                    a.resCap = a.cap;
                }
            }

        }

        /* initialize */
        for (Node i : nodes) {
            i.d = WHITE;
            buckets[i.bucketIndex].firstActive = null;
            i.current = 0;
        }

        /* eliminate flow cycles, topologicaly order vertices */
        Node r = null;
        Node restart = null;
        Node bos = null;
        Node tos = null;
        for (Node i : nodes)
            if ((i.d == WHITE) && (i.excess > 0) &&
                    (i != source) && (i != sink)) {
                r = i;
                r.d = GREY;
                do {
                    //for ( ; i->current != (i+1)->first; i->current++) {
                    for (; i.current != i.arcs.length; i.current++) {
                        Arc a = i.current();
                        //if (( cap[a - arcs] == 0 ) && ( a -> resCap > 0 )) {
                        if ((a.cap == 0) && (a.resCap > 0)) {
                            Node j = a.head;
                            if (j.d == WHITE) {
                                /* start scanning j */
                                j.d = GREY;
                                buckets[j.bucketIndex].firstActive = i;
                                i = j;
                                break;
                            } else if (j.d == GREY) {
                                /* find minimum flow on the cycle */
                                delta = a.resCap;
                                while (true) {
                                    delta = Math.min(delta, j.current().resCap);
                                    if (j == i)
                                        break;
                                    else
                                        j = j.current().head;
                                }

                                /* remove delta flow units */
                                j = i;
                                while (true) {
                                    a = j.current();
                                    a.resCap -= delta;
                                    a.rev.resCap += delta;
                                    j = a.head;
                                    if (j == i)
                                        break;
                                }

                                /* backup DFS to the first saturated arc */
                                restart = i;
                                for (j = i.current().head; j != i; j = a.head) {
                                    a = j.current();
                                    if ((j.d == WHITE) || (a.resCap == 0)) {
                                        j.current().head.d = WHITE;
                                        if (j.d != WHITE)
                                            restart = j;
                                    }
                                }

                                if (restart != i) {
                                    i = restart;
                                    i.current++;
                                    break;
                                }
                            }
                        }
                    }

                    //if (i->current == (i+1)->first) {
                    if (i.current == i.arcs.length) {
                        /* scan of i complete */
                        i.d = BLACK;
                        if (i != source) {
                            if (bos == null) {
                                bos = i;
                                tos = i;
                            } else {
                                i.bNext = tos;
                                tos = i;
                            }
                        }

                        if (i != r) {
                            i = buckets[i.bucketIndex].firstActive;
                            i.current++;
                        } else
                            break;
                    }
                } while (true);
            }


        /* return excesses */
        /* note that sink is not on the stack */
        if (bos != null) {
            for (Node i = tos; i != bos; i = i.bNext) {
                int ac = 0;
                while (i.excess > 0) {
                    Arc a = i.arcs[ac];
                    //if (( cap[a - arcs] == 0 ) && ( a -> resCap > 0 )) {
                    if ((a.cap == 0) && (a.resCap > 0)) {
                        if (a.resCap < i.excess)
                            delta = a.resCap;
                        else
                            delta = i.excess;
                        a.resCap -= delta;
                        a.rev.resCap += delta;
                        i.excess -= delta;
                        a.head.excess += delta;
                    }
                    ac++;
                }
            }
            /* now do the bottom */
            Node i = bos;
            int ac = 0;
            // todo : do we really have to move over all edges up to the end of the list
            // the original implementation runs to the end of the arc list,
            // starting with bos->first
            // if we do not run to the end we get one less relabeling operation but
            // the result stays the same. but this was just tested with the sample
            // from the original source package. we mimic the behaviour by collection
            // all edges starting at bos up to the end. This should give the
            // same structure as in the original implementations but may cost O(n) time and
            // O(2*m) extra memory
            //
            List<Arc> arcs = new ArrayList<Arc>();
            boolean bosFound = false;
            for (int j = 0; j < nodes.length; j++) {
                if (nodes[j] == i) {
                    bosFound = true;
                }
                if (bosFound) arcs.addAll(Arrays.asList(nodes[j].arcs));
            }

            while (i.excess > 0) {
                Arc a = arcs.get(ac);
                if ((a.cap == 0) && (a.resCap > 0)) {
                    if (a.resCap < i.excess)
                        delta = a.resCap;
                    else
                        delta = i.excess;
                    a.resCap -= delta;
                    a.rev.resCap += delta;
                    i.excess -= delta;
                    a.head.excess += delta;
                }
                ac++;
            }
        }
    }


/* gap relabeling */

    int gap(Bucket emptyB) {

        Bucket l;
        Node i;
        long r;           /* index of the bucket before l  */
        int cc;          /* cc = 1 if no nodes with positive excess before
                  the gap */

        gapCnt++;
        //r = ( emptyB - buckets ) - 1;
        r = emptyB.index - 1;

        /* set labels of nodes beyond the gap to "infinity" */
        //for ( l = emptyB + 1; l <= buckets + dMax; l ++ ) {

        //for ( l = emptyB + 1; l <= buckets + dMax; l ++ ) {
        for (int index = emptyB.index + 1; index <= dMax; index++) {
            /* this does nothing for high level selection
            for (i = l -> firstActive; i != sentinelNode; i = i -> bNext) {
              i -> d = n;
              gNodeCnt++;
            }
            l -> firstActive = sentinelNode;
            */
            l = buckets[index];
            for (i = l.firstInactive; i != null; i = i.bNext) {
                i.d = n;
                gNodeCnt++;
            }
            l.firstInactive = null;
        }
        cc = (aMin > r) ? 1 : 0;
        dMax = r;
        aMax = r;
        return (cc);
    }

/*--- relabelling node i */

    long relabel(Node i) {

        Node j;
        long minD;     /* minimum d of a node reachable from i */
        int minA;    /* an arc which leads to the node with minimal d */


        assert (i.excess > 0);

        relabelCnt++;
        workSinceUpdate += BETA;

        i.d = minD = n;
        minA = -1;

        /* find the minimum */
        int idx = 0;
        for (Arc a : i.arcs) {
            workSinceUpdate++;
            if (a.resCap > 0) {
                j = a.head;
                if (j.d < minD) {
                    minD = j.d;
                    minA = idx;
                }
            }
            idx++;
        }

        minD++;

        if (minD < n) {
            i.d = minD;
            i.current = minA;
            if (dMax < minD) dMax = minD;

        } /* end of minD < n */

        return (minD);

    } /* end of relabel */


/* discharge: push flow out of i until i becomes inactive */

    void discharge(Node i) {

        Node j;                 /* sucsessor of i */
        long jD;                 /* d of the next bucket */
        Bucket lj;               /* j's bucket */
        Bucket l;                /* i's bucket */
        int a;                 /* current arc (i,j) */
        long delta;
        int stopA;

        assert (i.excess > 0);
        assert (i != sink);
        do {

            jD = i.d - 1;
            //l = buckets + i->d;
            l = buckets[((int) i.d)];

            /* scanning arcs outgoing from  i  */
            //for (a = i.current, stopA = (i+1)->first; a != stopA; a++) {
            for (a = i.current, stopA = i.arcs.length; a != stopA; a++) {
                Arc arc = i.arcs[a];
                if (arc.resCap > 0) {
                    j = arc.head;

                    if (j.d == jD) {
                        pushCnt++;
                        if (arc.resCap < i.excess)
                            delta = arc.resCap;
                        else
                            delta = i.excess;
                        arc.resCap -= delta;
                        arc.rev.resCap += delta;

                        if (j != sink) {

                            //lj = buckets + jD;
                            lj = buckets[((int) jD)];

                            if (j.excess == 0) {
                                /* remove j from inactive list */
                                iDelete(lj, j);
                                /* add j to active list */
                                aAdd(lj, j);
                            }
                        }

                        j.excess += delta;
                        i.excess -= delta;

                        if (i.excess == 0) break;

                    } /* j belongs to the next bucket */
                } /* a  is not saturated */
            } /* end of scanning arcs from  i */

            if (a == stopA) {
                /* i must be relabeled */
                relabel(i);

                if (i.d == n) break;
                if ((l.firstActive == null) && // was sentinel
                        (l.firstInactive == null)
                        )
                    gap(l);

                if (i.d == n) break;
            } else {
                /* i no longer active */
                i.current = a;
                /* put i on inactive list */
                iAdd(l, i);
                break;
            }
        } while (true);
    }

/* first stage  -- maximum preflow*/

    void stageOne()

    {

        Node i;
        Bucket l;             /* current bucket */


        //#if defined(INIT_UPDATE) || defined(OLD_INIT) || defined(WAVE_INIT)
        //  globalUpdate ();
        //#endif

        workSinceUpdate = 0;

        //#ifdef WAVE_INIT
        //  wave();
        //#endif

        /* main loop */
        while (aMax >= aMin) {
            //l = buckets + aMax;
            l = buckets[((int) aMax)];
            i = l.firstActive;

            if (i == null) // was sentinelNode
                aMax--;
            else {
                aRemove(l, i);

                assert (i.excess > 0);
                discharge(i);

                if (aMax < aMin)
                    break;

                /* is it time for global update? */
                if (workSinceUpdate * globUpdtFreq > nm) {
                    globalUpdate();
                    workSinceUpdate = 0;
                }

            }

        } /* end of the main loop */
        flow = sink.excess;
    }


    public void setSink(Node sink) {
        this.sink = sink;
    }

    public void setSource(Node source) {
        this.source = source;
    }

    /**
     * Computes the mincut - this does only stageOne
     *
     * @param activateChecks check the results
     * @return cut all elements of the component that contains the sink
     */
    LinkedHashSet<Object> calculateMaxSTFlow(final boolean activateChecks) {
        long sum;
        globUpdtFreq = GLOB_UPDT_FREQ;
        this.source = source;
        this.sink = sink;


        allocDS();
        init();
        stageOne();

        if (activateChecks) {
            /* check if you have a flow (pseudoflow) */
            /* check arc flows */
            for (Node i : nodes) {
                for (Arc a : i.arcs) {
                    if (a.cap > 0) /* original arc */
                        if ((a.resCap + a.rev.resCap != a.cap)
                                || (a.resCap < 0)
                                || (a.rev.resCap < 0)) {
                            throw new RuntimeException("ERROR: bad arc flow\n");
                        }
                }
            }

            /* check conservation */
            for (Node i : nodes)
                if ((i != source) && (i != sink)) {
                    if (i.excess < 0) {
                        throw new RuntimeException("ERROR: nonzero node excess\n");
                    }
                    sum = 0;
                    for (Arc a : i.arcs) {
                        if (a.cap > 0) /* original arc */
                            sum -= a.cap - a.resCap;
                        else
                            sum += a.resCap;
                    }
                    if (i.excess != sum) {
                        throw new RuntimeException("ERROR: conservation constraint violated\n");
                    }
                }

            /* check if mincut is saturated */
            aMax = dMax = 0;
            for (Bucket bucket : buckets) {
                bucket.firstActive = null;
                bucket.firstInactive = null;
            }
            globalUpdate();
            if (source.d < n) {
                throw new RuntimeException("ERROR: the solution is not optimal\n");
            }
        }

        LinkedHashSet<Object> cut =  new LinkedHashSet<>();
        /// original :
        for (Node j : nodes) {
            if (j.d < n) {
                cut.add(j.name);
            }
        }
        return cut;
    }

    /*
    This was the coriginal main method --- somehow
     */
    /*void run(Node source, Node sink) {


        long sum;
        Bucket l;


        globUpdtFreq = GLOB_UPDT_FREQ;
        this.source = source;
        this.sink = sink;

        System.out.printf("c\nc hi_pr version 3.6\n");
        System.out.printf("c Copyright C by IG Systems, igsys@eclipse.net\nc\n");

        //parse( &n, &m, &nodes, &arcs, &cap, &source, &sink, &nMin );

        System.out.printf("c nodes:       %d\nc arcs:        %d\nc\n", n, m);

        allocDS();

        t = timer();
        t2 = t;

        init();
        stageOne();

        t2 = timer() - t2;

        System.out.printf("c flow:       %f\n", flow);

        //#ifndef CUT_ONLY
        stageTwo();

        t = timer() - t;

        System.out.printf("c time:        %f\n", t);

        //#endif

        System.out.printf("c cut tm:      %f\n", t2);

        *//* check if you have a flow (pseudoflow) *//*
        *//* check arc flows *//*
        for (Node i : nodes) {
            for (Arc a : i.arcs) {
                if (a.cap > 0) *//* original arc *//*
                    if ((a.resCap + a.rev.resCap != a.cap)
                            || (a.resCap < 0)
                            || (a.rev.resCap < 0)) {
                        System.err.printf("ERROR: bad arc flow\n");
                        System.exit(2);
                    }
            }
        }

        *//* check conservation *//*
        for (Node i : nodes)
            if ((i != source) && (i != sink)) {
                *//*
                #ifdef CUT_ONLY
                  if (i->excess < 0) {
                printf("ERROR: nonzero node excess\n");
                exit(2);
                  }
                #else
                *//*
                if (i.excess != 0) {
                    System.err.printf("ERROR: nonzero node excess\n");
                    System.exit(2);
                }


                sum = 0;

                for (Arc a : i.arcs) {

                    if (a.cap > 0) *//* original arc *//*
                        sum -= a.cap - a.resCap;
                    else
                        sum += a.resCap;
                }

                if (i.excess != sum) {
                    System.err.printf("ERROR: conservation constraint violated\n");
                    System.exit(2);
                }
            }

        *//* check if mincut is saturated *//*
        aMax = dMax = 0;
        for (Bucket bucket : buckets) {
            bucket.firstActive = null;
            bucket.firstInactive = null;
        }
//  for (l = buckets; l < buckets + n; l++) {
//    l->firstActive = sentinelNode;
//    l->firstInactive = sentinelNode;
//  }
        globalUpdate();
        if (source.d < n) {
            System.err.printf("ERROR: the solution is not optimal\n");
            System.exit(2);
        }

        System.out.printf("c\nc Solution checks (feasible and optimal)\nc\n");


        System.out.printf("c pushes:      %d\n", pushCnt);
        System.out.printf("c relabels:    %d\n", relabelCnt);
        System.out.printf("c updates:     %d\n", updateCnt);
        System.out.printf("c gaps:        %d\n", gapCnt);
        System.out.printf("c gap nodes:   %d\n", gNodeCnt);
        System.out.printf("c\n");


        System.out.printf("c flow values\n");
        for (Node i : nodes) {
            //ni = nNode(i);
            for (Arc a : i.arcs) {
                //na = nArc(a);
                if (a.cap > 0)
                    System.out.printf("f %s %s %d\n", i, a.head, a.cap - (a.resCap));
            }
        }
        System.out.printf("c\n");


        globalUpdate();
        System.out.printf("c nodes on the sink side\n");
        for (Node j : nodes)
            if (j.d < n)
                System.out.printf("c %s\n", j.toString());

    }*/

    long timer() {
        return System.currentTimeMillis();
    }


    /**
     * Returns the flow value
     *
     * @return flow the flow value
     */
    long getValue() {
        return flow;
    }

    /**
     * Internal edge representation
     */
    private class Arc {
        long cap;    /* capacity */
        long resCap;          /* residual capasity */
        Node head;           /* arc head */
        Arc rev;            /* reverse arc */

    }

    /**
     * Internal node representation
     */
    class Node {
        Arc[] arcs;           /* first outgoing arc */
        long excess;           /* excess at the node
                         change to double if needed */
        long d;                /* distance label */
        Node bNext;           /* next node in bucket */
        Node bPrev;           /* previous node in bucket */
        int current;          /* arc pointer */
        int bucketIndex;
        Object name;

        private int createdArcs = 0;

        Node(int edges, Object name) {
            this.name = name;
            this.arcs = new Arc[edges];
        }

        Arc current() {
            return arcs[current];
        }

        @Override
        public String toString() {
            return name.toString();
        }

        void addArc(Node head, long cap) {
            Arc a = new Arc();
            a.head = head;
            a.cap = cap;
            a.resCap = cap;
            addArc(a);


            Arc rev = new Arc();
            rev.head = this;
            rev.rev = a;
            a.rev = rev;
            head.addArc(rev);
        }

        private void addArc(Arc arc) {
            arcs[createdArcs++] = arc;
        }
    }

    /**
     * Internal data structure
     */
    private class Bucket {
        Node firstActive;      /* first node with positive excess */
        Node firstInactive;    /* first node with zero excess */
        int index;
    }


}
