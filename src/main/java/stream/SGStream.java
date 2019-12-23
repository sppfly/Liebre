package stream;

import common.scalegate.ScaleGate;
import common.scalegate.ScaleGateAArrImpl;
import common.scalegate.TuplesFromAll;
import component.StreamConsumer;
import component.StreamProducer;
import java.util.List;

/*
 * Assumption: all writers and reader threads are distinct
 * (i.e., this class is not safe when used in combination
 * with custom thread scheduling)
 */
public class SGStream<T extends Comparable<? super T>> extends AbstractStream<T> {

  public static final String SGSTREAM_UNSUPPORTED = "Cannot invoke this function on SGStream";

  // TODO Am I using id in the right way?
  // TODO Am I using index in the right way?
  // TODO Am I using enabled in the right way?
  // TODO Am I using sources in the right way?
  // TODO Am I using destinations in the right way?

  private ScaleGate<T> sg;
  private List<StreamProducer<T>> sources;
  private List<StreamConsumer<T>> destinations;

  private TuplesFromAll barrier;

  public SGStream(
      String id,
      int index,
      int maxLevels,
      int writers,
      int readers,
      List<StreamProducer<T>> sources,
      List<StreamConsumer<T>> destinations) {
    super(id, index);
    this.sg = new ScaleGateAArrImpl(maxLevels, writers, readers);
    this.sources = sources;
    this.destinations = destinations;

    barrier = new TuplesFromAll();
    barrier.setSize(writers);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public int getIndex() {
    return index;
  }

  @Override
  public void doAddTuple(T tuple, int writer) {
    sg.addTuple(tuple, writer);
  }

  @Override
  public T doGetNextTuple(int reader) {
    while (barrier.receivedTupleFromEachInput()) {}

    return sg.getNextReadyTuple(reader);
  }

  @Override
  public List<StreamProducer<T>> getSources() {
    return sources;
  }

  @Override
  public List<StreamConsumer<T>> getDestinations() {
    return destinations;
  }

  @Override
  public boolean offer(T tuple, int writer) {
    throw new UnsupportedOperationException(SGSTREAM_UNSUPPORTED);
  }

  @Override
  public T peek(int reader) {
    throw new UnsupportedOperationException(SGSTREAM_UNSUPPORTED);
  }

  @Override
  public int remainingCapacity() {
    return Integer.MAX_VALUE;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public void resetArrivalTime() {
    throw new UnsupportedOperationException(SGSTREAM_UNSUPPORTED);
  }

  @Override
  public double getAverageArrivalTime() {
    throw new UnsupportedOperationException(SGSTREAM_UNSUPPORTED);
  }
}
