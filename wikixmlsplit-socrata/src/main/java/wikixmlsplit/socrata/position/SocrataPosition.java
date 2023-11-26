package wikixmlsplit.socrata.position;

import wikixmlsplit.matching.position.Position;
import wikixmlsplit.matching.position.SimplePosition;
import wikixmlsplit.socrata.SocrataTable;

import java.util.function.BiFunction;
public class SocrataPosition extends SimplePosition {

	public SocrataPosition(int position) {
		super(position);
	}
	
	@Override
	public int getDifference(Position other) {
		int diff = super.getDifference(other);
		return Math.min(1, diff);
	}

	public static final BiFunction<SocrataTable, Integer, SocrataPosition> DEFAULT_MAPPER = (table,
			rank) -> new SocrataPosition(rank);
}
