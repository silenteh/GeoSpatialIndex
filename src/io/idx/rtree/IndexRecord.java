package io.idx.rtree;

import java.awt.Point;
import java.awt.Rectangle;

/*
 * This class is thread safe, because all the fields are immutable
 */

public class IndexRecord<T> {
	
	public final Rectangle r;
	public final T record;
	
	
	/*
	 * Constructor
	 */
	public IndexRecord(T record) {
		r = new Rectangle();
		this.record = record;
	}

	/*
	 * Constructor
	 */
	public IndexRecord(Point p,T record) {
		r = new Rectangle(0, 0, p.x, p.y);
		this.record = record;		
	}	
}
