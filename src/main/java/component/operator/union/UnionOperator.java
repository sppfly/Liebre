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

package component.operator.union;

import component.ComponentType;
import component.operator.AbstractOperator;
import stream.Stream;

/**
 * Operator that unites multiple input streams into one. No guarantee on the
 * ordering of the output tuples.
 *
 * @param <T> The type of input/output tuples.
 */
public class UnionOperator<T> extends AbstractOperator<T, T> {

    /**
     * Construct.
     *
     * @param id The unique ID of the component.operator.
     */
    public UnionOperator(String id) {
        super(id, ComponentType.UNION);
    }

    @Override
    protected final void process() {
        if (isFlushed()) {
            return;
        }
        Stream<T> output = getOutput();
        int finishedInputs = 0;
        for (Stream<T> in : getInputs()) {
            T inTuple = in.getNextTuple(getIndex());
            finishedInputs += isStreamFinished(inTuple, in) ? 1 : 0;
            if (inTuple != null) {
                increaseTuplesRead();
                increaseTuplesWritten();
                output.addTuple(inTuple, getIndex());
            }
        }
        if (finishedInputs == getInputs().size()) {
            flush();
            output.flush();
            return;
        }
    }

    @Override
    public void addInput(Stream<T> stream) {
        state.addInput(stream);
    }

    /**
     * Not meaningful in this component.operator, use {@link #getInputs()} instead.
     *
     * @throws UnsupportedOperationException always, since {@link UnionOperator} has
     *                                       multiple inputs.
     */
    @Override
    public Stream<T> getInput() {
        throw new UnsupportedOperationException(String.format("'%s': Unions have multiple inputs!", state.getId()));
    }

    @Override
    public boolean canRun() {
        if (getOutput().remainingCapacity() == 0) {
            return false;
        }
        for (Stream<?> input : getInputs()) {
            if (input.size() > 0) {
                return true;
            }
        }
        return false;
    }
}
