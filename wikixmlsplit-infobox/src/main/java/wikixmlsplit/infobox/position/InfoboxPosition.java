package wikixmlsplit.infobox.position;

import wikixmlsplit.infobox.Infobox;
import wikixmlsplit.matching.position.SimplePosition;

import java.util.function.BiFunction;

public class InfoboxPosition extends SimplePosition {

	public InfoboxPosition(int position) {
		super(position);
	}

	public static final BiFunction<Infobox, Integer, InfoboxPosition> DEFAULT_MAPPER = (table,
			rank) -> new InfoboxPosition(rank);

}
