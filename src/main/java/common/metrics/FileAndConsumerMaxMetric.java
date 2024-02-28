package common.metrics;

import java.util.function.Consumer;

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

/**
 * Statistic that records the per-second max of the recorded value.
 * Assumes non-negative records.
 */
public class FileAndConsumerMaxMetric extends AbstractFileAndConsumerMetric {
  private long max;
  long prevSec;
  boolean resetCount;

  private final long missingValue = -1;

  // Notice in this case the last max value must become the new neutral value in
  // case of missing value
  private long neutralValue = Long.MIN_VALUE;

  public FileAndConsumerMaxMetric(String id, String folder, boolean autoFlush, boolean resetCount,
      Consumer<Object[]> c) {
    super(id, folder, autoFlush, c);
    this.resetCount = resetCount;
  }

  @Override
  protected void doRecord(long v) {
    writePreviousCounts();
    if (max == missingValue) {
      max = neutralValue;
    }
    max = Long.max(max, v);
    neutralValue = max;
  }

  @Override
  public void enable() {
    this.max = missingValue;
    this.prevSec = currentTimeSeconds();
    super.enable();
  }

  public void disable() {
    writePreviousCounts();
    super.disable();
  }

  private void writePreviousCounts() {
    long thisSec = currentTimeSeconds();
    while (prevSec < thisSec) {
      writeCSVLineAndConsume(prevSec, max);
      if (resetCount) {
        neutralValue = Long.MIN_VALUE;
      }
      max = missingValue;
      prevSec++;
    }
  }

  @Override
  public void reset() {
    max = missingValue;
    neutralValue = Long.MIN_VALUE;
  }

  @Override
  public void ping() {
    writePreviousCounts();
  }
}
