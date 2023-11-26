package wikixmlsplit.webtable.position;

import wikixmlsplit.matching.position.SimplePosition;
import wikixmlsplit.webtable.Webtable;

import java.util.function.BiFunction;

public class WebtablePosition extends SimplePosition {

	public WebtablePosition(int position) {
		super(position);
	}

	public static final BiFunction<Webtable, Integer, WebtablePosition> DEFAULT_MAPPER = (table,
			rank) -> new WebtablePosition(rank);
}
