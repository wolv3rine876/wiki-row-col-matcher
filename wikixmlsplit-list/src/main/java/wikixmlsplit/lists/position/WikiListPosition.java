package wikixmlsplit.lists.position;

import wikixmlsplit.lists.WikiList;
import wikixmlsplit.matching.position.SimplePosition;

import java.util.function.BiFunction;

public class WikiListPosition extends SimplePosition {

	public WikiListPosition(int position) {
		super(position);
	}

	public static final BiFunction<WikiList, Integer, WikiListPosition> DEFAULT_MAPPER = (list,
			rank) -> new WikiListPosition(rank);

}
