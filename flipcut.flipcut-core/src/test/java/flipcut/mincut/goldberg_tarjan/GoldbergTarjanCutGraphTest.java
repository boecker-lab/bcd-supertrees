package flipcut.mincut.goldberg_tarjan;/*
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
 * along with Epos.  If not, see <http://www.gnu.org/licenses/>
 */

import flipcut.mincut.bipartition.BasicCut;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Thasso Griebel (thasso.griebel@gmail.com)
 */
public class GoldbergTarjanCutGraphTest {

    @Test
    public void testExample(){
        GoldbergTarjanCutGraph hp = new GoldbergTarjanCutGraph();
        hp.addNode(1);
        hp.addNode(2);
        hp.addNode(3);
        hp.addNode(4);
        hp.addNode(5);
        hp.addNode(6);
        hp.addNode(7);
        hp.addNode(8);


        hp.addEdge(1, 2, 5);
        hp.addEdge(2, 3, 5);
        hp.addEdge(3, 4, 5);
        hp.addEdge(3, 5, 2);
        hp.addEdge(4, 2, 5);
        hp.addEdge(4, 7, 2);
        hp.addEdge(5, 6, 5);
        hp.addEdge(6, 6, 5);
        hp.addEdge(6, 8, 4);
        hp.addEdge(7, 5, 5);
        hp.addEdge(7, 8, 1);

        BasicCut cut = hp.calculateMinSTCut(1, 8);
        System.out.println(cut.minCutValue);
        System.out.println(cut.getCutSet());


        assertEquals(4, cut.minCutValue);
        assertEquals(4, cut.getCutSet().size());
        assertTrue(cut.getCutSet().contains(5));
        assertTrue(cut.getCutSet().contains(6));
        assertTrue(cut.getCutSet().contains(7));
        assertTrue(cut.getCutSet().contains(8));

        /*
        p max 8 11
        n 1 s
        n 8 t
        a 1 2 5
        a 2 3 5
        a 3 4 5
        a 3 5 2
        a 4 2 5
        a 4 7 2
        a 5 6 5
        a 6 7 5
        a 6 8 4
        a 7 5 5
        a 7 8 1
        */

    }
}
