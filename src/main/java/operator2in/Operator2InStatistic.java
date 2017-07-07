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

package operator2in;

import statistic.AvgStat;
import stream.Stream;
import tuple.Tuple;

public class Operator2InStatistic<T1 extends Tuple, T2 extends Tuple, T3 extends Tuple>
		implements Operator2In<T1, T2, T3> {

	protected Stream<T1> in1;
	protected Stream<T2> in2;
	protected Stream<T3> out;
	protected boolean active = false;
	private BaseOperator2In<T1, T2, T3> operator;
	private AvgStat processingTimeStat;

	public Operator2InStatistic(BaseOperator2In<T1, T2, T3> operator,
			String outputFile) {
		this.operator = operator;
		this.processingTimeStat = new AvgStat(outputFile, true);
	}

	public Operator2InStatistic(BaseOperator2In<T1, T2, T3> operator,
			String outputFile, boolean autoFlush) {
		this.operator = operator;
		this.processingTimeStat = new AvgStat(outputFile, autoFlush);
	}

	@Override
	public void registerIn1(Stream<T1> in) {
		this.in1 = in;
		this.operator.registerIn1(in);
	}

	@Override
	public void registerIn2(Stream<T2> in) {
		this.in2 = in;
		this.operator.registerIn2(in);
	}

	@Override
	public void registerOut(Stream<T3> out) {
		this.out = out;
		this.operator.registerOut(out);
	}

	@Override
	public void run() {
		while (active) {
			process();
		}
	}

	@Override
	public void activate() {
		active = true;
	}

	@Override
	public void deActivate() {
		processingTimeStat.close();
		active = false;
	}

	protected void process() {
		long start = System.nanoTime();
		this.operator.process();
		processingTimeStat.add(System.nanoTime() - start);
	}
}
