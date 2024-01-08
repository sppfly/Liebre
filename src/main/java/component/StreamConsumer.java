/*
 * Copyright (C) 2017-2019
 *   Vincenzo Gulisano
 *   Dimitris Palyvos-Giannas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 *   Vincenzo Gulisano info@vincenzogulisano.com
 *   Dimitris Palyvos-Giannas palyvos@chalmers.se
 */

package component;

import common.tuple.RichTuple;
import common.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import scheduling.haren.HarenFeatureTranslator;
import stream.Stream;

/**
 * A stream {@link Component} that consumes tuples.
 *
 * @param <IN> The input type for this component.
 */
public interface StreamConsumer<IN> extends Named, Component {

    /**
     * Connect this consumer with the given {@link StreamProducer} using the
     * provided stream. Different implementations allow one or more calls to this
     * function.
     *
     * @param stream The {@link Stream} that forms the data connection.
     * @see ConnectionsNumber
     */
    void addInput(Stream<IN> stream);

    /**
     * Get the input {@link Stream} of this consumer, if is the type of consumer
     * that always has a unique input. {@link StreamConsumer}s that cannot conform
     * to this interface can throw {@link UnsupportedOperationException} (this is
     * done for example in {@link component.operator.union.UnionOperator})
     *
     * @return The unique input stream of this consumer.
     */
    Stream<IN> getInput() throws UnsupportedOperationException;

    /**
     * Get all the input {@link Stream}s of this consumer.
     *
     * @param <T> The superclass of all input contents (in the case of input streams
     *            of different types, as in
     *            {@link component.operator.in2.Operator2In}.
     * @return All the input streams of this consumer.
     */
    <T> Collection<? extends Stream<T>> getInputs();

    @Override
    default int getTopologicalOrder() {
        int maxUpstreamOrder = 0;
        for (Stream<?> input : getInputs()) {
            for (StreamProducer<?> source : input.producers()) {
                maxUpstreamOrder = Math.max(source.getTopologicalOrder(), maxUpstreamOrder);
            }
        }
        return maxUpstreamOrder + 1;
    }

    @Override
    default List<Component> getUpstream() {
        List<Component> upstream = new ArrayList<>();
        for (Stream<?> input : getInputs()) {
            for (StreamProducer<?> op : input.producers()) {
                upstream.add(op);
            }
        }
        return upstream;
    }

    /**
     * Get the latency at the head of the queue of the component. Warning: This will
     * fail if the component is not processing {@link common.tuple.RichTuple}s.
     *
     * @return The head latency of the Component (averaged over all the inputs).
     */
    @Override
    default double getHeadArrivalTime() {
        Collection<? extends Stream<?>> inputs = getInputs();
        long latencySum = 0;
        for (Stream<?> input : inputs) {
            Object head = input.peek(getIndex());
            if (head != null) {
                if (head instanceof RichTuple == false) {
                    // This stream has no latency info
                    continue;
                }
                RichTuple headTuple = (RichTuple) head;
                latencySum += headTuple.getStimulus();
            }
        }
        return latencySum <= 0 ? HarenFeatureTranslator.NO_ARRIVAL_TIME : latencySum / inputs.size();
    }

    @Override
    default double getAverageArrivalTime() {
        Collection<? extends Stream<?>> inputs = getInputs();
        long latencySum = 0;
        for (Stream<?> input : inputs) {
            latencySum += input.averageArrivalTime();
        }
        return latencySum <= 0 ? HarenFeatureTranslator.NO_ARRIVAL_TIME : latencySum / inputs.size();
    }

    default int getPriority() {
        int priority = -1;
        // Consumer's priority is the maximum priority of the upstream nodes
        for (Stream<?> input : getInputs()) {
            for (StreamProducer<?> s : input.producers()) {
                priority = Math.max(s.getPriority(), priority);
            }
        }
        return priority;
    }

    @Override
    default long getInputQueueSize() {
        long size = 0;
        for (Stream<?> input : getInputs()) {
            size += input.size();
        }
        return size;
    }

}
