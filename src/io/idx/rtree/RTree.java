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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/*
 * Reference for the implementation:
 * http://www-db.deis.unibo.it/courses/SI-LS/papers/Gut84.pdf
 * 
 */

public class RTree<T> {

	// parameters of the tree
	private final static int DEFAULT_MAX_NODE_ENTRIES = 50;
	private int nodeMaxSize;
	private int nodeMinSize;
	private Node rootNode;
	private final boolean isLeaf = true;  
	
	
	
	final static ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
	
	
	// constructor which initialize the object
	public RTree(int nodeMaxSize) {
		if(nodeMaxSize <= 5) {
			this.nodeMaxSize = DEFAULT_MAX_NODE_ENTRIES;			
		} else {
			this.nodeMaxSize = nodeMaxSize;
		}
		nodeMinSize = (int)(nodeMaxSize * 0.45);
		rootNode = new Node(nodeMaxSize, nodeMinSize, isLeaf, null);		
	}
	
	/** 
	 *  This method search inside the tree for a point
	 *
	 * @param  longitude the longitude of the point 
	 * @param  latitude the latitude of the point
	 * @return      a list of indexed records stored in the tree
	 */
	
	@SuppressWarnings("unchecked")
	public List<T> search(double longitude, double latitude) {
		
		Point p = new Point(this.increaseUnit(longitude),this.increaseUnit(latitude));
		// minimum bounding box of point p		
		Rectangle mbb = new Rectangle(0, 0, p.x, p.y);
		
		// I am using here a thread safe array, because multiple thread can write to the same list
		// it is quite expensive because it make a copy of the array on each write
		// however the write rate here is supposedly low
		List<T> result = new CopyOnWriteArrayList<T>();
		List<Future<List<T>>> tasks = new ArrayList<Future<List<T>>>();
		
		if(!rootNode.isLeaf) {
			for(IndexRecord<?> r : rootNode.entries ) {
				if(mbb.contains(r.r) || mbb.intersects(r.r)) {
					IndexRecord<Node> entry = (IndexRecord<Node>)r;					
					//qualifiedRecords.addAll(search(p,entry.record));
					
					tasks.add(es.submit(new SearchSolver<T>(entry.record, p, result)));
				}
			}
			
			for(Future<List<T>> task : tasks) {
				try {
					task.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
			
		} else { // [S2]			
			for(IndexRecord<?> r : rootNode.entries ) {
				if(mbb.contains(r.r) || mbb.intersects(r.r)) {
					if(r.r.equals(mbb)) {
						result.add((T) r.record);
					}
				}
			}			
		}				
				
		return result;
	}
	
	/** 
	 *  Add a record to the tree.
	 *
	 * @param  record the object you want to store
	 * @param  longitude the longitude associated with the record 
	 * @param  latitude the latitude associated with the record
	 * @return      void
	 */
	public void addIndex(T record, double latitude, double longitude) {
		
		int l = (int)(Math.abs(latitude) * 100000);
		int ll = (int)(Math.abs(longitude) * 100000);
		Point p = new Point(l,ll);		
		IndexRecord<T> r = new IndexRecord<T>(p,record);
		this.addRecord(r);
	}
	
	/** 
	 *  Internal method which adds the record to the tree
	 *
	 * @param  record the IndexRecord to add
	 * @return      void
	 */
	public void addRecord(IndexRecord<T> record) {
		
		/*
		 * [ I1 ]
		 * [ Find position for the new record  ] 
		 * Invoke  ChooseLeaf  to  select  a  leaf node  L,
		 * in  which  to  place  E 
		 */					
		Node n = chooseLeaf(rootNode, record);		
		Node newLeaf = null;
		
		/* 
		 * [ I2 ]
		 * [ Add  record  to  leaf  node ]
		 * If  L  has room  for  another  entry, install(add)  E 
		 * otherwise  invoke  SplitNode  to  obtain
		 * L(node)  and  LL(node) containing E  and all the old entries of L
		 */
		if(n.hasSpace()) {
			n.add(record);
		} else {
			// add in any case the entry because it will get split later, 
			// the array in the node is already built with a capacity of MAX ENTRIES + 1
			// so the performances should be reasonable
			n.add(record);
			// Now Split Node
			newLeaf = this.splitNode(n);
		}				
		
		/*
		 * [Propagate  changes  upward]  Invoke
		 * AdjustTree  on  L,  also  passing LL  if a not split happened
		 *  
		 */
		
		// adjust tree structure and bounding boxes
		Node newNode = this.adjustTree(n, newLeaf);		
		
		// means the initial root node is Leaf and has not space anymore
		// therefore it has been split
		// we need to change the root node with a new one containing the split
		// this happens only when the root node is a LEAF (which is when a new RTree object has been initialized and there are
		// entries < max
		// in other words this code is executed only once ! when the rootNode overflows
		if(newLeaf != null && rootNode.isLeaf) {
			
			// create a empty root node			
			Node newRoot = new Node(nodeMaxSize, nodeMinSize, false, null);
			
			// set the current root node, rootNode property to the newly created root node
			rootNode.setRootNode(newRoot);
			
			// create a new node record with the old root
			// because the old root contains all the database records indexes
			// and add it to the newly created rootNode
			// calculate the BB
			IndexRecord<Node> oldNodeRecord = new IndexRecord<Node>(rootNode);
			newRoot.add(oldNodeRecord);
			this.calculateMBB(rootNode);
			
			// do the same as above but for the newLeaf node
			// which comes from the overflowing of the rootNode
			// the newLeaf has to have its rootNode set to the new root node we create above (called newRoot)
			newLeaf.setRootNode(newRoot);
			IndexRecord<Node> newNodeRecord = new IndexRecord<Node>(newLeaf);
			newRoot.add(newNodeRecord);
			this.calculateMBB(newLeaf);
			
			// assign the RTree rootNode object to the new one we just populated
			rootNode = newRoot;
		}		
		
		/*
		 * [ I4 ]
		 * 
		 * [Grow  tree  taller  ]  If  node  split  propagation
		 * caused  the  root  to  split
		 * create  a  new  root  whose  children  are 
		 * the  two  resulting  nodes 
		 * 
		 */
		if(newNode != null && !rootNode.isLeaf ) {
			
			IndexRecord<Node> newSplittedNode = new IndexRecord<Node>(this.calculateMaxPoint(newNode),newNode);
			rootNode.add(newSplittedNode);
			
			for(IndexRecord<?> index : newNode.entries) {
				rootNode.remove(index);
			}
		}
		
		// DEBUG ONLY
		//System.out.println("-------------------------------------");
		//System.out.println(rootNode.hashCode());
		//this.print(rootNode.entries, "");
		
	}
	
	/*
	 * This method print the TREE
	 * WARNING: if the tree is big this will take a lot of time and resources !
	 * it should be used only for debugging purposes
	 */
	private void print(List<IndexRecord<?>> entries, String separator) {
		
		if(entries == null) {
			entries = this.rootNode.entries;
		}
		
		String space = separator + " - ";
		for(IndexRecord<?> r : entries) {
			if(r.record instanceof Node) {
				Node n = (Node)r.record;
				System.out.println(space + n.hashCode() + " (" + r.r.width + "," + r.r.height + ")");
				this.print(n.entries,space);
			} else {
				Record entry = (Record)r.record;
				System.out.println(space + entry.id);
			}
		}
	}
	
	
	/*
	 * TODO: we should give a try to JDK 7 fork/join
	 * This is the private method to search inside the TREE
	 */
	@SuppressWarnings("unchecked")
	private List<T> search(Point p, Node node, List<T> qualifiedRecords) {
		
		if(node == null) {
			node = rootNode;
		}
		
		if(qualifiedRecords == null) {
			qualifiedRecords = new ArrayList<T>();
		}
		
		// minimum bounding box of point p		
		Rectangle mbb = new Rectangle(0, 0, p.x, p.y);
		if(!node.isLeaf) {
			for(IndexRecord<?> r : node.entries ) {
				if(mbb.contains(r.r) || mbb.intersects(r.r)) {
					IndexRecord<Node> entry = (IndexRecord<Node>)r;
					search(p,entry.record,qualifiedRecords);
				}
			}
		} else { // [S2]			
			for(IndexRecord<?> r : node.entries ) {
				if(mbb.contains(r.r) || mbb.intersects(r.r)) {
					if(r.r.equals(mbb)) {
						qualifiedRecords.add((T) r.record);
					}
				}
			}			
		}
		return qualifiedRecords;
	}
	
	
	
	
	
	/*
	 * Choose Leaf:
	 * this is called only internally, during the add method
	 */
	//@SuppressWarnings("unchecked")
	private Node chooseLeaf(Node node, IndexRecord<?> record) {
				
		// if is leaf add
		if(node.isLeaf) {
			return node;
		} else {
			
			// because the node is NOT a leaf then all the entries are references to other nodes
			
			// Use the first record to calculate the first enlargement			
			// compare it with an empty in terms of dimensions, rectangle
			long leastEnlargement = this.enlargement(new Rectangle(), node.entries.get(0).r);
			
			// the index in the cycle with the least enlargement and the smallest area
			int bestIndex = 0;
			
			// max number of loops
			int loopLimit = node.entries.size();
			
			// var for the cycle - Watch out !
			// the var starts from 1 ! NOT from 0
			// because we used the first entry as reference
			int i;
			for(i = 1; i < loopLimit; i++) {
				
				//calculate the union and the total area of the 2 rectangles
				// use the previous rectangle and the current one
				// this works because we start the cycle from 1 ! 
				long enlargement = this.enlargement(record.r, node.entries.get(i).r);
				
				// if the new area is smaller or equal than the stored one then:
				// - check if it is equal and take the index of the rectangle with the smallest area
				if(enlargement <= leastEnlargement) {
					
					if(enlargement == leastEnlargement) {						
						//take the index with the smallest area
						if(this.area(node.entries.get(i).r) < this.area(node.entries.get(bestIndex).r)) {
							bestIndex = i;
						}
					} else {
						leastEnlargement = enlargement;
						bestIndex = i;
					}
				}
			}
			
			Node n = (Node) node.entries.get(bestIndex).record;
			if(n.isLeaf) {
				return n;
			} else {
				return chooseLeaf(n, record);
			}
		}		
	}
	
	
	private Node splitNode(Node n) {
		
		// group 1 is the origin node		
		Node group1 = n;
		// the root node of the new node, is the parent of Node n
		Node group2 = new Node(nodeMaxSize, nodeMinSize, n.isLeaf, n.getRootNode());				
		
		// picks the seeds
		int[] first2Entries = this.pickSeeds(n);		
		int group2Entry = first2Entries[0];
		int group1Entry = first2Entries[1];
		
		// get the records		
		IndexRecord<?> record1 = n.entries.get(group1Entry);
		IndexRecord<?> record2 = n.entries.get(group2Entry);
		
		// assign the seed to the second group
		// the first group has already the seed so no point to remove it
		group2.add(record2);
		
		// remove the seed assigned to group 2 from group 1
		group1.remove(record2);
		
		// copy the entries in another array so we can safely process them
		ArrayList<IndexRecord<?>> copyOfEntries = new ArrayList<IndexRecord<?>>(group1.entries);
		
		// get the rectangle of each entry
		Rectangle group1Rectangle = record1.r;
		Rectangle group2Rectangle = record2.r;
		
		
		int totalEntries = copyOfEntries.size();
		
		int i;
		for(i=0; i < totalEntries; i++) {
			
			// if the second group reached already the minimum size
			// we can exit the loop
			if(group2.entries.size() >= group2.nodeMinSize) {
				break;
			}
			
			int nextEntryIndex = this.pickNext(copyOfEntries, group1Rectangle, group2Rectangle);
			IndexRecord<?> nextRecord = copyOfEntries.get(nextEntryIndex);
			short group = this.detectGroupAssignement(nextRecord, group1Rectangle, group2Rectangle, 
					group1.entries.size(), group2.entries.size());
			
			if(group == 3) { // means both				
				
				// add to both groups
				// group 1 already has the entry so no point to add it				
				this.addToGroup(group2, nextRecord);
				
			} else if(group == 2) { // means group2
				//add to group2
				this.addToGroup(group2, nextRecord);
				
				// remove it from group 1
				group1.remove(nextRecord);

			} // else would mean add it to group 1, but it contains the record already so nothing to do
			
			copyOfEntries.remove(nextRecord);
		}
		
		return group2;
	}
	
	// add the node to the right group
	private void addToGroup(Node n, IndexRecord<?> nextRecord ) {
		if(!n.entries.contains(nextRecord)) {
			n.add(nextRecord);
		}
	}
	
	// detect in which group the node falls
	private short detectGroupAssignement(IndexRecord<?> record , Rectangle group1, Rectangle group2, 
		int group1Size, int group2Size) {
		
		short group = 1;
		
		// detect which rectangle has to be enlarged the least between group1 and group 2 
		// rectangles and the record
		
		long enlarged1 = this.enlargement(group1, record.r);
		long enlarged2 = this.enlargement(group2, record.r);
		
		if(enlarged1 == enlarged2) {
			// both groups
			group = 3;
			
			long areaGroup1 = this.area(group1); 
			long areaGroup2 = this.area(group2);
			
			if(areaGroup1 < areaGroup2) {
				group = 1;
			} else if(areaGroup1 > areaGroup2) {
				group = 2;
			} else {
				if(group1Size > group2Size) {
					group = 2;
				} else if(group1Size < group2Size) {
					group = 1;
				}
			}
			
		} else if(enlarged1 > enlarged2) {
			group = 2;
		} // else the group is = 1, but because we have set the variable we do not have to do anything 				
		
		return group;
	}
	
	
	
	/*
	 * PickSeeds
	 */
	private int[] pickSeeds(Node n) {
		
		// max number of loops
		int loopLimit = n.entries.size();
		
		long wasteFull = Long.MIN_VALUE;
		int[] watseFullPairsIndex = new int[2];
		
		int i;
		for(i = 0; i < loopLimit; i++) {
						
			Seed seed = this.combine(n, i, wasteFull);
			if(seed.wasteFull > wasteFull) {
				wasteFull = seed.wasteFull;
				watseFullPairsIndex[0] = seed.bestIndex[0];
				watseFullPairsIndex[1] = seed.bestIndex[1];
			}			
		}
		
		return watseFullPairsIndex;
	}
	
	/*
	 * Get the best index from the combination of all entries
	 */
	private Seed combine(Node n, int startIndex, long wasteFull) {
		
		IndexRecord<?> r1 = n.entries.get(startIndex);
		int limit = n.entries.size();
		int start = startIndex + 1;
		int i;
		
		Seed seed = new Seed();
		
		for(i=start; i < limit; i++) {
						
			IndexRecord<?> r2 = n.entries.get(i);
			
			long areaR1	= this.area(r1.r);
			long areaR2	= this.area(r2.r);
			
			//first we need to compose a rectangle which include the 2 of them			
			Rectangle unionRectangle = r1.r.union(r2.r);  // now r1 is the UNION of r1 and r2 rectangles
			
			long areaUnion = this.area(unionRectangle);			
			long d = Math.abs(areaUnion - areaR1 - areaR2);
			
			if(d > wasteFull) {
				seed.bestIndex[0] = startIndex;
				seed.bestIndex[1] = i;
				seed.wasteFull = d;
				
			}			
		}
		
		return seed;
	}
	
	
	/*
	 * PickNext
	 * in the list parameter the assigned entries must have been removed
	 */
	
	private int pickNext(List<IndexRecord<?>> entries, Rectangle group1, Rectangle group2) {
		
		long maxDifference = Long.MIN_VALUE;
		int bestIndex = 0;
		
		for(IndexRecord<?> record : entries) {
			
			//calculate area increase required to cover the group1 rectangle and the group 2 rectangle
			
			Rectangle r = (Rectangle) record.r;			
			long d1 = this.enlargement(group1, r);
			long d2 = this.enlargement(group2, r);
			
			long difference = Math.abs(d1 - d2);
			if(difference > maxDifference) {
				maxDifference = difference;
				bestIndex = entries.indexOf(record);
			}
		}
	
		return bestIndex;
		
	}
	
	
	/*
	 * Adjust Tree
	 */
	private Node adjustTree(Node n, Node nn) {
		
		// the root node has its root node == null
		// because it is the first node and it does not have parents
		Node newNode = null;
		while(n.getRootNode() != null) {
			
			// [AT3]
			// get the root node of N
			Node parent = n.getRootNode();		
			// fix the enclosing bounding box
			this.calculateMBB(n);						
			
			// assign NN to the root node entries if there is space and calculate bounding box
			// [AT4]
			
			if(nn != null) {
				IndexRecord<Node> record = new IndexRecord<Node>(this.calculateMaxPoint(nn), nn);
				if(parent.hasSpace()) {
					parent.add(record);
				}
				else {  //if not space split
					// but still add the record, because it will get split on splitNode and we are using arraylist
					// which auto increments (even though the performance might suffer from it)
					parent.add(record);
					newNode = splitNode(parent);
				}
			}
			
			// assign to n variable the parent so we go from the bottom to the top
			n = parent;
			nn = newNode;
		}
		
		return newNode;
	}
	
	
	/*
	 * Calculate minimum bounding box of all entries of a Node
	 * 
	 */	
	private void calculateMBB(Node child) {
						
		Point maxPoint = this.calculateMaxPoint(child);
		
		Node parent = child.getRootNode();
		int limit = parent.entries.size();
		int i;
		
		for(i=0; i < limit; i++) {
			
			IndexRecord<?> nodeRecord = parent.entries.get(i);			
									
			if(nodeRecord.record.equals(child)) {				
				IndexRecord<Node> newIndex = new IndexRecord<Node>(maxPoint, child);
				parent.entries.set(i, newIndex);				
				break;
			}
		}
	}
	
	/*
	 * Get the max point between all entries of a node
	 */
	private Point calculateMaxPoint(Node node) {				
		
		int maxX = node.xMax();
		int maxY = node.yMax();
		return new Point(maxX, maxY);
	}
	
	/*
	 * This calculate the union of 2 rectangle and the total area
	 */
	private long enlargement(Rectangle baseR, Rectangle newR) {
		
		if(baseR.contains(newR)) {
			//System.out.println("the second rectangle is contained !!! - the result will be zero !");
			return 0;
		}
		
		Rectangle r = baseR.union(newR);
		
		long baseArea = this.area(baseR);
		long extendedArea = this.area(r);
		
		return Math.abs(extendedArea - baseArea);
	}
	
	/*
	 * Calculate rectangle area
	 */
	private long area(Rectangle r) {
		return (long)r.height * (long)r.width;
	}
	
	//Private inner class for the Seed object
	class Seed {
		public int[] bestIndex = new int[2];
		public long wasteFull;
	}
	
	/*
	 *  We want to work with INT
	 *  therefore I multiply the doubles I get from the client side as coordinates
	 *  so we can work with INTEGERS only
	 *  We keep the precision up to a 1 meter (  0.5 < x < 0.5 ) at the equator
	 */
	private int increaseUnit(double value) {
		return (int)(Math.abs(value) * 100000);		
	}
}
