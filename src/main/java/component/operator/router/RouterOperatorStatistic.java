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

import common.statistic.LiebreMetrics;
import io.palyvos.liebre.statistics.StatisticsFactory;
import common.statistic.StatisticType;
import io.palyvos.liebre.statistics.TimeStatistic;
import java.util.Collection;
import stream.Stream;

/**
 * Statistic decorator for {@link RouterOperator}. Records, in separate CSV files, {@link
 * StatisticType#PROC} and {@link StatisticType#EXEC}
 *
 * @see StatisticPath
 */
public class RouterOperatorStatistic<T> extends RouterOperatorDecorator<T> {

  private final StatisticsFactory statisticsFactory = LiebreMetrics.statistiscFactory();
  private final TimeStatistic processingTimeStatistic;
  private final TimeStatistic executionTimeStatistic;

  /**
   * Add statistics to the given component.operator.
   *
   * @param operator The component.operator to add statistics to
   * @param outputFolder The folder where the statistics will be saved as CSV files
   * @param autoFlush The autoFlush parameter for the file buffers
   */
  public RouterOperatorStatistic(
      RouterOperator<T> operator, String outputFolder, boolean autoFlush) {
    super(operator);
    this.processingTimeStatistic =
        statisticsFactory.newAverageTimeStatistic(getId(), StatisticType.PROC);
    this.executionTimeStatistic =
        statisticsFactory.newAverageTimeStatistic(getId(), StatisticType.EXEC);
  }

  @Override
  public void enable() {
    super.enable();
    processingTimeStatistic.enable();
    executionTimeStatistic.enable();
  }

  @Override
  public void disable() {
    processingTimeStatistic.disable();
    executionTimeStatistic.disable();
    super.disable();
  }

  @Override
  public Collection<? extends Stream<T>> chooseOutputs(T tuple) {
    processingTimeStatistic.startInterval();
    Collection<? extends Stream<T>> chosenOutputs = super.chooseOutputs(tuple);
    processingTimeStatistic.stopInterval();
    return chosenOutputs;
  }

  @Override
  public void run() {
    executionTimeStatistic.startInterval();
    super.run();
    executionTimeStatistic.stopInterval();
  }
}
