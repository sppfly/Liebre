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

package component.operator.router;

import common.tuple.RichTuple;
import component.ComponentType;
import component.operator.AbstractOperator;
import stream.Stream;

import java.util.Collection;

/**
 * Default implementation for {@link RouterOperator}.
 *
 * @param <T> The type of input/output tuples.
 */
public class HashBasedRouterOperator<T extends RichTuple> extends AbstractOperator<T, T> implements RouterOperator<T> {

    private boolean firstInvocation = true;
    Stream<T>[] outArray;

    public HashBasedRouterOperator(String id) {
        super(id, ComponentType.ROUTER);
    }

    @Override
    protected final void process() {

        if (isFlushed()) {
            return;
        }

        if (firstInvocation) {
            firstInvocation = false;
            outArray = new Stream[getOutputs().size()];
            int index = 0;
            for (Stream<T> output : getOutputs()) {
                outArray[index] = output;
                index++;
            }
        }

        Stream<T> input = getInput();
        T inTuple = input.getNextTuple(getIndex());

        if (isStreamFinished(inTuple, input)) {
            flush();
            return;
        }

        if (inTuple != null) {
            increaseTuplesRead();
            increaseTuplesWritten();
            int i = (int) (inTuple.getKey().hashCode() % outArray.length);
            outArray[i].addTuple(inTuple, getIndex());
        }
    }

    @Override
    public Collection<? extends Stream<T>> chooseOutputs(T tuple) {
        assert (false);
        return null;
    }

    @Override
    public void addOutput(Stream<T> stream) {
        state.addOutput(stream);
    }

    public Stream<T> getOutput() {
        throw new UnsupportedOperationException(String.format("'%s': Router has multiple outputs!", state.getId()));
    }

    @Override
    public boolean canRun() {
        if (getInput().size() == 0) {
            return false;
        }
        for (Stream<?> output : getOutputs()) {
            if (output.remainingCapacity() > 0) {
                return true;
            }
        }
        return false;
    }
}
