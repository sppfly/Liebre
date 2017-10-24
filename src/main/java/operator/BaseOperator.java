/*  Copyright (C) 2017  Vincenzo Gulisano
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Contact: Vincenzo Gulisano info@vincenzogulisano.com
 *
 */

package operator;

import java.util.List;

import common.tuple.Tuple;
import stream.Stream;

public abstract class BaseOperator<IN extends Tuple, OUT extends Tuple> implements Operator<IN, OUT> {

	protected Stream<IN> in;
	protected Stream<OUT> out;
	protected boolean active = false;
	private final String id;

	public BaseOperator(String id) {
		this.id = id;
	}

	@Override
	public void registerIn(String id, Stream<IN> in) {
		this.in = in;
	}

	@Override
	public void registerOut(String id, Stream<OUT> out) {
		this.out = out;
	}

	@Override
	public void run() {
		if (active) {
			process();
		}
	}

	@Override
	public void activate() {
		active = true;
	}

	@Override
	public void deActivate() {
		active = false;
	}

	public boolean isActive() {
		return active;
	}

	public void process() {
		IN inTuple = in.getNextTuple();
		if (inTuple != null) {
			List<OUT> outTuples = processTuple(inTuple);
			if (outTuples != null) {
				for (OUT t : outTuples)
					out.addTuple(t);
			}
		}
	}

	@Override
	public synchronized long getPriority() {
		return in != null ? in.size() : 0;
	}

	@Override
	public String getId() {
		return this.id;
	}

	public abstract List<OUT> processTuple(IN tuple);

	@Override
	public String toString() {
		return String.format("OP-%s [priority=%d]", id, getPriority());
	}
}
