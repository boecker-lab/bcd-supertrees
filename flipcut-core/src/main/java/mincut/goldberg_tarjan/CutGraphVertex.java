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

package mincut.goldberg_tarjan;

/**
 * Created by IntelliJ IDEA.
 * User: fleisch
 * Date: 17.08.2010
 * Time: 13:18:30
 * To change this template use File | Settings | File Templates.
 */
public class CutGraphVertex {

    private int index = -1;

    private boolean leaf = false;

    public CutGraphVertex(int index, boolean isLeaf){
        this.index =  index;
        this.leaf = isLeaf;

    }

    public boolean isLeaf(){
        return leaf;
    }

    public int getIndex(){
        return index;
    }





}
