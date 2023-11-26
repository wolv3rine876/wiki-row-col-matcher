package wikixmlsplit.evaluation.gsreader;

import wikixmlsplit.infobox.Infobox;
import wikixmlsplit.infobox.builder.InfoboxBuilder;
import wikixmlsplit.infobox.position.InfoboxPosition;

import java.util.List;
import java.util.Map;

public class GSReaderInfobox extends GSReader<Infobox, InfoboxPosition> {
	private final InfoboxBuilder infoboxBuilder = new InfoboxBuilder();
	
	@Override
	protected InfoboxPosition getPosition(int pos) {
		 return new InfoboxPosition(pos);
	}
	
	@Override
	protected List<Infobox> getNewObjects(Map<String, List<String>> nodes) {
		return infoboxBuilder.constructNewObjects(nodes);
	}
}
