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

import common.component.Component;
import common.tuple.Tuple;
import common.util.Backoff;
import common.util.ExponentialBackoff;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Deprecated
public class ConcurrentLinkedListStream<T extends Tuple> implements Stream<T> {

  private static final long WRITER_BACKOFF_LIMIT = 50;
  private static final long WRITER_RELAX_LIMIT = 25;
  private static AtomicInteger nextIndex = new AtomicInteger();
  private final Backoff writerBackOff, readerBackOff;
  private final String id;
  private final int index;
  private final Component source;
  private final Component destination;
  private ConcurrentLinkedQueue<T> stream = new ConcurrentLinkedQueue<T>();
  private volatile long tuplesWritten, tuplesRead;
  private volatile boolean enabled;

  public ConcurrentLinkedListStream(String id, Component source, Component destination) {
    this.id = id;
    this.index = nextIndex.getAndIncrement();
    this.source = source;
    this.destination = destination;
    writerBackOff = new ExponentialBackoff(10, 3);
    readerBackOff = new ExponentialBackoff(1, 2);
    tuplesWritten = 0;
    tuplesRead = 0;
  }

  @Override
  public void addTuple(T tuple) {
    if (!isEnabled()) {
      return;
    }
    if (size() > WRITER_BACKOFF_LIMIT) {
      writerBackOff.backoff();
    } else if (size() < WRITER_RELAX_LIMIT) {
      writerBackOff.relax();
    }
    stream.add(tuple);
    tuplesWritten++;
  }

  @Override
  public boolean offer(T tuple) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T getNextTuple() {
    if (!isEnabled()) {
      return null;
    }
    T nextTuple = stream.poll();
    if (nextTuple == null) {
      readerBackOff.backoff();
    } else {
      readerBackOff.relax();
      tuplesRead++;
    }
    return nextTuple;
  }

  @Override
  public T poll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void enable() {
    this.enabled = true;
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  @Override
  public void disable() {
    this.enabled = false;
  }

  @Override
  public T peek() {
    return isEnabled() ? stream.peek() : null;
  }

  @Override
  public long size() {
    return tuplesWritten - tuplesRead;
  }

  @Override
  public long remainingCapacity() {
    return Long.MAX_VALUE;
  }

  @Override
  public String getId() {
    return this.id;
  }

  public Component getSource() {
    return source;
  }

  public Component getDestination() {
    return destination;
  }

  @Override
  public int getIndex() {
    return index;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ConcurrentLinkedListStream)) {
      return false;
    }
    ConcurrentLinkedListStream<?> other = (ConcurrentLinkedListStream<?>) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "ConcurrentLinkedListStream [id=" + id + ", tuplesWritten=" + tuplesWritten
        + ", tuplesRead="
        + tuplesRead + ", writerBackOff=" + writerBackOff + ", readerBackOff=" + readerBackOff
        + "]";
  }

}
