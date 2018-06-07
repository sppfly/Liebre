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

package stream;

import common.statistic.CountStatistic;
import common.tuple.Tuple;
import common.util.StatisticFilename;

public class StreamStatistic<T extends Tuple> extends StreamDecorator<T> {

  private final CountStatistic inRate;
  private final CountStatistic outRate;

  public StreamStatistic(Stream<T> stream, String outputFolder, boolean autoFlush) {
    super(stream);
    inRate = new CountStatistic(StatisticFilename.INSTANCE.get(outputFolder, stream, "in"),
        autoFlush);
    outRate = new CountStatistic(StatisticFilename.INSTANCE.get(outputFolder, stream, "out"),
        autoFlush);
  }

  @Override
  public void addTuple(T tuple) {
    inRate.append(1);
    super.addTuple(tuple);
  }

  @Override
  public boolean offer(T tuple) {
    boolean offered = super.offer(tuple);
    if (offered) {
      inRate.append(1);
    }
    return offered;
  }

  @Override
  public T poll() {
    T tuple = super.poll();
    if (tuple != null) {
      outRate.append(1);
    }
    return tuple;
  }

  @Override
  public T getNextTuple() {
    T out = super.getNextTuple();
    if (out != null) {
      outRate.append(1);
    }
    return out;
  }

  @Override
  public void enable() {
    super.enable();
    inRate.enable();
    outRate.enable();
  }

  @Override
  public void disable() {
    inRate.disable();
    outRate.disable();
    super.disable();
  }

}
