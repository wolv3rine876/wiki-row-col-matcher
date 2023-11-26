package wikixmlsplit.evaluation.gsreader;

import wikixmlsplit.wikitable.WikiTable;
import wikixmlsplit.wikitable.builder.TableBuilder;
import wikixmlsplit.wikitable.position.TablePosition;

import java.util.List;
import java.util.Map;

public class GSReaderTable extends GSReader<WikiTable, TablePosition> {
	private final TableBuilder tableBuilder = new TableBuilder(true);
	
	@Override
	protected TablePosition getPosition(int pos) {
		 return new TablePosition(pos);
	}
	
	@Override
	protected List<WikiTable> getNewObjects(Map<String, List<String>> nodes) {
		return tableBuilder.constructNewObjects(nodes);
	}
}
