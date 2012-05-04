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
