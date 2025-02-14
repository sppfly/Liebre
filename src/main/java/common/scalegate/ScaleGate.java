/*  Copyright (C) 2015  Ioannis Nikolakopoulos,
 * 			Daniel Cederman,
 * 			Vincenzo Gulisano,
 * 			Marina Papatriantafilou,
 * 			Philippas Tsigas
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
 *  Contact: Ioannis (aka Yiannis) Nikolakopoulos ioaniko@chalmers.se
 *  	     Vincenzo Gulisano vincenzo.gulisano@chalmers.se
 *
 */

package common.scalegate;

public interface ScaleGate<T extends Comparable<? super T>> {

    // Called by each processing thread to get the next ready tuple
    T getNextReadyTuple(int readerID);

    // Just add a tuple (used for the output tuples TGate)
    void addTuple(T tuple, int writerID);

    // Note that this implementation is not synchronized. The behavior is not
    // specified if addTuple method is invoked
    // by any thread after this method has been invoked
    void letItFlush();

    boolean hasBeenEmptied();

}
