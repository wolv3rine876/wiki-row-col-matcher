package wikixmlsplit.evaluation.gsreader;

import wikixmlsplit.lists.WikiList;
import wikixmlsplit.lists.builder.WikiListBuilder;
import wikixmlsplit.lists.position.WikiListPosition;

import java.util.List;
import java.util.Map;

public class GSReaderWikiList extends GSReader<WikiList, WikiListPosition> {
	private final WikiListBuilder listBuilder = new WikiListBuilder();
	
	@Override
	protected WikiListPosition getPosition(int pos) {
		 return new WikiListPosition(pos);
	}
	
	@Override
	protected List<WikiList> getNewObjects(Map<String, List<String>> nodes) {
		return listBuilder.constructNewObjects(nodes);
	}
}
