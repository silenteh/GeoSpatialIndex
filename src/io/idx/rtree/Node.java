/**
 * Copyright [2012] [Silenteh]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.idx.rtree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/*
 * 
 */

public class Node {
	
	public final boolean isLeaf;
	public final int nodeMaxSize;
	public final int nodeMinSize;
		
	private Set<Integer> xSet;
	private Set<Integer> ySet;
	public final List<IndexRecord<?>> entries;
	
	private Node rootNode;
	
	
	public Node(int nodeMaxSize, int nodeMinSize, boolean isLeaf, Node rootNode) {
		this.nodeMaxSize = nodeMaxSize;
		this.nodeMinSize = nodeMinSize;
		this.isLeaf = isLeaf;		
		this.rootNode = rootNode;
		this.entries = new ArrayList<IndexRecord<?>>(nodeMaxSize + 1);
		xSet = Collections.synchronizedSet(new HashSet<Integer>(nodeMaxSize + 1));
		ySet = Collections.synchronizedSet(new HashSet<Integer>(nodeMaxSize + 1));
						
	}				
	
	public boolean hasSpace() {
		if(this.entries.size() < this.nodeMaxSize) {
			return true;
		}
		return false;
	}

	public synchronized Node getRootNode() {
		return rootNode;
	}

	public synchronized void setRootNode(Node rootNode) {
		this.rootNode = rootNode;
	}
	
	public void add(IndexRecord<?> record) {
		this.entries.add(record);		
		this.xSet.add(record.r.width);
		this.ySet.add(record.r.height);
	}
	
	public void remove(IndexRecord<?> record) {
		this.entries.remove(record);
		this.xSet.remove(record.r.width);
		this.ySet.remove(record.r.height);
	}
	
	public int xMax() {
		if(xSet.size() == 0) {
			return 0;
		}
		return Collections.max(this.xSet);
	}
	
	public int yMax() {
		if(ySet.size() == 0) {
			return 0;
		}
		return Collections.max(this.ySet);		
	}
	
}
