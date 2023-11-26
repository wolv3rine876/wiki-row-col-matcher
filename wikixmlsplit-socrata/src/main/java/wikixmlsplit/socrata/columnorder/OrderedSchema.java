package wikixmlsplit.socrata.columnorder;

import java.util.List;

public class OrderedSchema {

	private String id;
	private List<String> columns;

	public OrderedSchema(String id, List<String> columns) {
		super();
		this.id = id;
		this.columns = columns;
	}

	public String getId() {
		return id;
	}

	public List<String> getColumns() {
		return columns;
	}
}
