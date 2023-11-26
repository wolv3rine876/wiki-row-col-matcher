package wikixmlsplit.wikitable.position;

import wikixmlsplit.matching.position.SimplePosition;
import wikixmlsplit.wikitable.WikiTable;

import java.util.function.BiFunction;

public class TablePosition extends SimplePosition {

	public TablePosition(int position) {
		super(position);
	}

	public static final BiFunction<WikiTable, Integer, TablePosition> DEFAULT_MAPPER = (table,
			rank) -> new TablePosition(rank);
}
