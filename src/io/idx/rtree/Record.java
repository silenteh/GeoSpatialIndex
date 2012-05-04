package io.idx.rtree;


/*
 * This class is used for testing purposed only
 */
public class Record {
	
	public int id;
	public double latitude;
	public double longitude;
	
	public Record(int id) {
		this.id = id;
	}
	
	public Record(int id, double longitude, double latitude) {
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
	}

}
