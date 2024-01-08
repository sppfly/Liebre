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

package component.operator.in1.map;

import component.operator.in1.BaseOperator1In;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * {@link component.operator.Operator} that applies a given {@link MapFunction}
 * to each tuple of its input stream.
 *
 * @param <IN>  The type of input tuples.
 * @param <OUT> The type of output tuples.
 */
public class MapOperator<IN, OUT> extends BaseOperator1In<IN, OUT> {

    private MapFunction<IN, OUT> map;

    /**
     * Construct.
     *
     * @param id  The unique id of this component.operator.
     * @param map The {@link MapFunction} to be applied to every input tuple.
     */
    public MapOperator(String id, MapFunction<IN, OUT> map) {
        super(id);
        Validate.notNull(map, "map");
        this.map = map;
    }

    @Override
    public void enable() {
        map.enable();
        super.enable();
    }

    @Override
    public void disable() {
        super.disable();
        map.disable();
    }

    @Override
    public List<OUT> processTupleIn1(IN tuple) {
        List<OUT> result = new LinkedList<OUT>();
        OUT t = map.apply(tuple);
        if (t != null) {
            result.add(t);
        }
        return result;
    }

    @Override
    public boolean canRun() {
        return map.canRun() && super.canRun();
    }
}
