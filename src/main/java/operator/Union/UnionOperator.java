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

package operator.Union;

import common.StreamProducer;
import common.component.ComponentType;
import common.tuple.Tuple;
import operator.AbstractOperator;
import scheduling.priority.PriorityMetric;
import stream.Stream;
import stream.StreamFactory;

public class UnionOperator<T extends Tuple> extends AbstractOperator<T, T> {
	private PriorityMetric metric = PriorityMetric.noopMetric();

	public UnionOperator(String id, StreamFactory streamFactory) {
		super(id, ComponentType.UNION);
	}

	@Override
	public void addInput(StreamProducer<T> source, Stream<T> stream) {
	  state.addInput(stream);
	}

	@Override
	public Stream<T> getInput() {
	  throw new UnsupportedOperationException(String.format("'%s': Unions have multiple inputs!", state.getId()));
	}

	@Override
	public void run() {
		if (isEnabled()) {
			process();
		}
	}

	// TODO: Convert to command like the other operators
	public final void process() {
    Stream<T> output = getOutput();
		for (Stream<T> in : state.getInputs()) {
			T inTuple = in.getNextTuple();
			if (inTuple != null) {
				metric.recordTupleRead(inTuple, in);
				metric.recordTupleWrite(inTuple, output);
				output.addTuple(inTuple);
			}
		}
	}


	@Override
	public void setPriorityMetric(PriorityMetric metric) {
		this.metric = metric;
	}

}
