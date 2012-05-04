package io.idx.rtree;

public class InsertSolver<T> implements Runnable{

	IndexRecord<T> record;
	RTree<T> index;
	
	public InsertSolver(IndexRecord<T> record, RTree<T> index) {
		this.record = record;
		this.index = index;
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		index.addRecord(record);
		
	}

}
