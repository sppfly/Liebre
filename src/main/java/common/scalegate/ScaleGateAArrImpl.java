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

import java.util.Random;

public class ScaleGateAArrImpl<T extends Comparable<T>> implements ScaleGate<T> {

    final int maxlevels;
    SGNodeAArrImpl<T> head;
    final SGNodeAArrImpl<T> tail;

    final int numberOfWriters;
    final int numberOfReaders;
    // Arrays of source/reader id local data
    WriterThreadLocalData[] writertld;
    ReaderThreadLocalData[] readertld;

    public ScaleGateAArrImpl(int maxlevels, int writers, int readers) {
        this.maxlevels = maxlevels;

        this.head = new SGNodeAArrImpl(maxlevels, null, null, -1);
        this.tail = new SGNodeAArrImpl(maxlevels, null, null, -1);

        for (int i = 0; i < maxlevels; i++)
            head.setNext(i, tail);

        this.numberOfWriters = writers;
        this.numberOfReaders = readers;

        writertld = new WriterThreadLocalData[numberOfWriters];
        for (int i = 0; i < numberOfWriters; i++) {
            writertld[i] = new WriterThreadLocalData(head, maxlevels);
        }

        readertld = new ReaderThreadLocalData[numberOfReaders];
        for (int i = 0; i < numberOfReaders; i++) {
            readertld[i] = new ReaderThreadLocalData(head);
        }

        // This should not be used again, only the writer/reader-local variables
        head = null;
    }

    @Override
    /*
     * (non-Javadoc)
     */
    public T getNextReadyTuple(int readerID) {
        SGNodeAArrImpl next = getReaderLocal(readerID).localHead.getNext(0);

        if (next != tail && !next.isLastAdded()) {
            getReaderLocal(readerID).localHead = next;
            return (T) next.getTuple();
        }
        return null;
    }

    @Override
    // Add a tuple
    public void addTuple(T tuple, int writerID) {
        this.internalAddTuple(tuple, writerID);
    }

    @Override
    public void letItFlush() {
        for (int i = 0; i < numberOfWriters; i++) {
            getWriterLocal(i).written = tail;
        }
    }

    @Override
    public boolean hasBeenEmptied() {
        for (int i = 0; i < numberOfReaders; i++)
            if (getReaderLocal(i).localHead.getNext(0) != tail)
                return false;
        return true;
    }

    private void insertNode(SGNodeAArrImpl fromNode, SGNodeAArrImpl newNode, final T obj, final int level) {
        while (true) {
            SGNodeAArrImpl next = fromNode.getNext(level);
            if (next == tail || next.getTuple().compareTo(obj) > 0) {
                newNode.setNext(level, next);
                if (fromNode.trySetNext(level, next, newNode)) {
                    break;
                }
            } else {
                fromNode = next;
            }
        }
    }

    private SGNodeAArrImpl internalAddTuple(T obj, int inputID) {
        int levels = 1;
        WriterThreadLocalData ln = getWriterLocal(inputID);

        while (ln.rand.nextBoolean() && levels < maxlevels)
            levels++;

        SGNodeAArrImpl newNode = new SGNodeAArrImpl(levels, obj, ln, inputID);
        SGNodeAArrImpl[] update = ln.update;
        SGNodeAArrImpl curNode = update[maxlevels - 1];

        for (int i = maxlevels - 1; i >= 0; i--) {
            SGNodeAArrImpl tx = curNode.getNext(i);

            while (tx != tail && tx.getTuple().compareTo(obj) < 0) {
                curNode = tx;
                tx = curNode.getNext(i);
            }

            update[i] = curNode;
        }

        for (int i = 0; i < levels; i++) {
            this.insertNode(update[i], newNode, obj, i);
        }

        ln.written = newNode;
        return newNode;
    }

    private WriterThreadLocalData getWriterLocal(int writerID) {
        return writertld[writerID];
    }

    private ReaderThreadLocalData getReaderLocal(int readerID) {
        return readertld[readerID];
    }

    protected static class WriterThreadLocalData<T extends Comparable<T>> {
        // reference to the last written node by the respective writer
        volatile SGNodeAArrImpl<T> written;
        SGNodeAArrImpl<T>[] update;
        final Random rand;

        public WriterThreadLocalData(SGNodeAArrImpl<T> localHead, int maxlevels) {
            update = new SGNodeAArrImpl[maxlevels];
            written = localHead;
            for (int i = 0; i < maxlevels; i++) {
                update[i] = localHead;
            }
            rand = new Random();
        }
    }

    protected static class ReaderThreadLocalData<T extends Comparable<T>> {
        SGNodeAArrImpl<T> localHead;

        public ReaderThreadLocalData(SGNodeAArrImpl<T> lhead) {
            localHead = lhead;
        }
    }
}
