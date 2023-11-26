package wikixmlsplit.wikirow.position;

import java.util.function.BiFunction;

import wikixmlsplit.matching.position.SimplePosition;
import wikixmlsplit.wikirow.WikiTuple;

public class ElementPosition extends SimplePosition {

	public ElementPosition(int position) {
		super(position);
	}

	public static final BiFunction<WikiTuple, Integer, ElementPosition> DEFAULT_MAPPER = (table,
			rank) -> new ElementPosition(rank);
}