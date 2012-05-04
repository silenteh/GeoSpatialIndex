package io.idx.rtree;
import io.idx.rtree.RTree;
import io.idx.rtree.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.Test;


public class RtreeTest {
	
	// in order to check the behavior with different node size, change this
	private final static int MAX_NODE_SIZE = 50;
	
	private final static int MAX_FAKE_ENTRIES = 4000000;
	
	// ---------------------------------------------------------------------
	// the RTree object
	private static RTree<Record> index = new RTree<Record>(MAX_NODE_SIZE);
	private static Random rnd = new Random();
	private static List<Record> fakeData = new ArrayList<Record>();
	
	@BeforeClass
	public static void init() {				
		
		System.out.println("Starting generation of random coordinates");
		long start = System.currentTimeMillis();
		Record r;
		int i;
		for(i = 0; i< MAX_FAKE_ENTRIES; i++) {
			
			double x = generateCoordinate();
			double y = generateCoordinate();
			
			r = new Record(i,x,y);
			fakeData.add(r);
			
		}
		long end = System.currentTimeMillis();
		
		System.out.println("Generation finished in: " + (end -start));
		
	}

	@Test
	public void test() {
		
		for(int i=0; i < 1; i++) {
			this.executeTest();
		}
		
		int selectedPoint = MAX_FAKE_ENTRIES - 30;
		if(selectedPoint < 0) {
			selectedPoint = 0;
		}
		
		
		Record r = fakeData.get(selectedPoint);
		
		System.out.println("Selected point: lat: " + r.latitude +" - long: "+ r.longitude + " - id: "+r.id);
		
		long start = System.currentTimeMillis();
		
		List<Record> result = index.search(r.longitude, r.latitude);
		
		long end = System.currentTimeMillis();
		
		System.out.println("-----------");
		System.out.println("Search finished in: " + (end -start));
		
		
		
		
		
		for(Record entry : result) {
			System.out.println(entry.id);
			System.out.println(entry.latitude);
			System.out.println(entry.longitude);
		}
		
		
		
		//index.print(null, "");
		
		// 50.08804 / 14.42076
		
		/*index.addIndex(r,50.08804,14.42076);
		index.addIndex(r,50.08805,14.42077);
		index.addIndex(r,50.08806,14.42078);
		index.addIndex(r,50.08807,14.42079);
		index.addIndex(r,50.08808,14.42080);
		index.addIndex(r,50.08809,14.42081);
		index.addIndex(r,50.08810,14.42082);
		index.addIndex(r,50.08811,14.42083);
		index.addIndex(r,50.08812,14.42084);
		index.addIndex(r,50.08813,14.42085);
		index.addIndex(r,50.08814,14.42086);
		index.addIndex(r,50.08815,14.42087);
		index.addIndex(r,50.08816,14.42088);
		*/
		
		
		/*index.addIndex(new Record(1),120.08816,24.42088);
		index.addIndex(new Record(2),130.08816,34.42088);
		index.addIndex(new Record(3),40.08816,14.42088);
		index.addIndex(new Record(4),80.08816,4.42088);
		index.addIndex(new Record(5),32.08816,22.42088);
		index.addIndex(new Record(6),65.08816,17.42088);
		index.addIndex(new Record(7),87.08816,88.42088);
		index.addIndex(new Record(8),21.08816,78.42088);
		index.addIndex(new Record(9),11.08816,61.42088);
		index.addIndex(new Record(10),8.08816,43.42088);
		index.addIndex(new Record(11),50.08804,14.42076);
		index.addIndex(new Record(12),50.08816,14.42088);
		index.addIndex(new Record(13),80.08816,5.42088);
		index.addIndex(new Record(14),81.08816,9.42088);
		
		index.addIndex(new Record(15),4.08816,11.42088);
		index.addIndex(new Record(16),11.08816,45.42088);
		index.addIndex(new Record(17),120.08816,89.42088);
		index.addIndex(new Record(18),132.08816,52.42088);
		index.addIndex(new Record(19),34.08816,29.42088);
		index.addIndex(new Record(20),84.08816,48.42088);
		index.addIndex(new Record(21),142.08816,51.42088);
		index.addIndex(new Record(22),3.08816,76.42088);
		
		index.addIndex(new Record(23),5.08816,35.42088);
		index.addIndex(new Record(24),13.08816,12.42088);
		index.addIndex(new Record(25),172.08816,7.42088);
		index.addIndex(new Record(26),99.08816,6.42088);
		*/
		
		
		//System.out.println("TEMP");
		
		//fail("Not yet implemented");
	}
	
	private static double generateCoordinate() {
		int intPart = rnd.nextInt(89);
		double decPart = rnd.nextDouble();
		
		return intPart * decPart;
	}
	
	
	private void executeTest() {
		
		long start = System.currentTimeMillis();
		
		for(Record r : fakeData) {
			index.addIndex(r, r.longitude, r.latitude);
		}
		
		long end = System.currentTimeMillis();
		
		System.out.println(end -start);
	}
	
}
