package query;

import common.NamedEntity;
import common.tuple.Tuple;
import stream.ConcurrentLinkedListStream;
import stream.Stream;
import stream.StreamFactory;

public enum ConcurrentLinkedListStreamFactory implements StreamFactory {
	INSTANCE;

	@Override
	public <T extends Tuple> Stream<T> newStream(NamedEntity from, NamedEntity to) {
		return new ConcurrentLinkedListStream<T>(String.format("%s_%s", from.getId(), to.getId()), from, to);
	}

}
