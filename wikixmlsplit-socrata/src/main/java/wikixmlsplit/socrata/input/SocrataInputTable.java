package wikixmlsplit.socrata.input;

import wikixmlsplit.socrata.SocrataTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class SocrataInputTable {
	public String id;
	public String version;
	public Attribute[] attributes;
	public SocrataTableRow[] rows;

	public SocrataTable toSocrataTable(double attrNameWeight, boolean sortRows, MetaData meta) {
		List<List<String>> cells = getRows(sortRows);
		
		List<String> attrNameRow = getAttributeNames();
		
		return new SocrataTable(attrNameRow, cells, meta);
	}

	protected List<List<String>> getRows(boolean sortRows) {
		List<List<String>> cells = new ArrayList<>();

		for (SocrataTableRow row : rows) {
			ArrayList<String> curRow = new ArrayList<>();
			cells.add(curRow);
			for (Object field : row.fields) {
				if (field == null)
					curRow.add("");
				else
					curRow.add(field.toString());
			}
		}

		if (sortRows) {
			sortRows(cells);
		}
		return cells;
	}

	protected ArrayList<String> getAttributeNames() {
		ArrayList<String> attrNameRow = new ArrayList<>();
		for (Attribute attr : attributes) {
			attrNameRow.add(attr.name);
		}
		return attrNameRow;
	}

	protected void sortRows(List<List<String>> cells) {
		Comparator<List<String>> comp = Comparator.comparing(listGetter(0));
		for (int i = 1; i < attributes.length; ++i) {
			comp = comp.thenComparing(listGetter(i));
		}
		cells.sort(comp);
	}

	protected Function<? super List<String>, ? extends String> listGetter(int index) {
		return l -> l.get(index);
	}

}