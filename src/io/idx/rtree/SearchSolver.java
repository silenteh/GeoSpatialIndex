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

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.Callable;

public class SearchSolver<T> implements Callable<List<T>>{


	private final Node node;
	private final Point p;
	private final Rectangle mbb;
	private final List<T> qualifiedRecords;

	
	
	public SearchSolver(Node node, Point point, List<T> qualifiedRecords) {
		this.node = node;
		this.p = point;
		mbb = new Rectangle(0, 0, p.x, p.y);
		this.qualifiedRecords = qualifiedRecords;
		
	}
	
	
	@Override
	public List<T> call() throws Exception {
		// TODO Auto-generated method stub
		this.search(p, node, qualifiedRecords);
		return qualifiedRecords;
	}
	
	
	@SuppressWarnings("unchecked")
	private void search(Point p, Node node, List<T> qualifiedRecords) {				
		
		// minimum bounding box of point p		
		//Rectangle mbb = new Rectangle(0, 0, p.x, p.y);
		
		
		
		
		// S1 [search subtree]
		// T = root node of the RTree
		// if T is NOT a leaf, check all entries and see if mbb overlaps
		// For all overlapping entries call SEARCH
		
		// S2 [search leaf node]
		// if T is leaf, check all entries and see if mbb overlaps
		// if yes then it is a qualified record
		
		//qualifiedRecords.addAll(new SearchSolver<T>(node, p).invoke());
		// [S1]
		// not the best to "!" a boolean, but I want to follow the comments order
		if(!node.isLeaf) {
			for(IndexRecord<?> r : node.entries ) {
				if(mbb.contains(r.r) || mbb.intersects(r.r)) {
					IndexRecord<Node> entry = (IndexRecord<Node>)r;					
					//qualifiedRecords.addAll(search(p,entry.record));
					search(p,entry.record,qualifiedRecords);
				}
			}
		} else { // [S2]			
			for(IndexRecord<?> r : node.entries ) {
				if(mbb.contains(r.r) || mbb.intersects(r.r)) {					
					//IndexRecord<?> entry = (IndexRecord<?>)r;
					//IndexRecord<T> entry = (IndexRecord<T>)r;
					if(r.r.equals(mbb)) {
						qualifiedRecords.add((T) r.record);
					}
				}
			}			
		}
		//return Collections.unmodifiableList(qualifiedRecords);
		//return qualifiedRecords;
	}

}
