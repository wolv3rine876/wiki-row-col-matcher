package wikixmlsplit.wikitable;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.sweble.wikitext.parser.nodes.WtNode;
import wikixmlsplit.matching.similarity.BagStore;
import wikixmlsplit.matching.similarity.SchemaObject;

import java.util.*;

public class WikiTable implements SchemaObject {
	private String caption;
	private List<List<Cell>> rows;
	private String headings;

	private WtNode node;
	private Multiset<String> bagOfWords = null;

	private Multiset<String> bagOfWordsFirstLine = null;
	private String filename;

	public WikiTable(String caption, List<List<Cell>> rows, boolean useSpans) {
		super();
		this.caption = caption;
		this.rows = rows;

		if (!isEmpty()) {
			int maxColumns = rows.stream().mapToInt(List::size).max().getAsInt();
			if (caption == null && rows.size() > 0 && rows.get(0).size() > 0) {
				Object colspan = this.rows.get(0).get(0).getProperty("colspan");
				if (colspan != null && ((Integer) colspan) >= maxColumns) {
					this.caption = rows.get(0).get(0).getValue();
				}
			}

			this.rows = buildRows(rows, useSpans, maxColumns);
		}
	}

	private List<List<Cell>> buildRows(List<List<Cell>> rows, boolean useSpans, int maxColumns) {
		Map<Integer, ActiveSpan> spans = new HashMap<>();
		List<List<Cell>> newRows = new ArrayList<>();
		int maxColumn = 0;
		for (List<Cell> cells : rows) {
			int column = 0;
			Iterator<Cell> cellIter = cells.iterator();
			List<Cell> newRow = new ArrayList<>();
			while (cellIter.hasNext()) {
				column = useSpans(spans, column, newRow);

				Cell cell = cellIter.next();
				int colspan = useSpans && cell.getProperty("colspan") != null
						? Integer.parseInt("" + cell.getProperty("colspan"))
						: 1;
				int rowspan = useSpans && cell.getProperty("rowspan") != null
						? Integer.parseInt("" + cell.getProperty("rowspan"))
						: 1;
				if (rowspan > 1) {
					spans.put(column, new ActiveSpan(rowspan - 1, colspan, cell));
				}
				for (int i = 0; i < colspan; ++i) {
					if (newRow.size() >= maxColumns)
						break;
					newRow.add(cell);
					++column;
				}

			}
			for (; column < maxColumn; ++column) {
				column = useSpans(spans, column, newRow);
			}
			if (newRow.size() > maxColumn) {
				maxColumn = newRow.size();
			}
			newRows.add(newRow);
		}
		return newRows;
	}

	private int useSpans(Map<Integer, ActiveSpan> spans, int column, List<Cell> newRow) {
		while (spans.containsKey(column)) {
			ActiveSpan s = spans.get(column);
			for (int i = 0; i < s.getColumns(); ++i) {
				newRow.add(s.getCell());
				++column;
			}

			if (s.takeRow())
				spans.remove(column - s.getColumns());
		}
		return column;
	}

	public void setHeadings(String headings) {
		this.headings = headings;
	}

	private static BagStore<Multiset<String>> sets = new BagStore<>();

	public Multiset<String> getBagOfValues(int limit, boolean includeHeaders) {
		if (bagOfWords == null) {
			Multiset<String> set = HashMultiset.create();
			if (includeHeaders && headings != null) {
				String[] words = headings.split("[^A-Za-z0-9]+", limit + 1);
				for (int i = 0; i < Math.min(words.length, limit); ++i) {
					set.add(words[i], 1);
				}
			}
			if (caption != null) {
				String[] words = caption.split("[^A-Za-z0-9]+", limit + 1);
				for (int i = 0; i < Math.min(words.length, limit); ++i) {
					set.add(words[i], 1);
				}
			}
			for (List<Cell> row : rows) {
				addTokens(limit, set, row);
			}
			set.remove("", set.count(""));
			bagOfWords = sets.get(set);
		}

		return bagOfWords;
	}

	public Multiset<String> getSchemaBoW(int limit) {
		if (bagOfWordsFirstLine == null) {
			Multiset<String> set = HashMultiset.create();
			for (List<Cell> row : rows) {
				addTokens(limit, set, row);
				break;
			}
			set.remove("", set.count(""));
			bagOfWordsFirstLine = sets.get(set);
		}

		return bagOfWordsFirstLine;
	}

	private void addTokens(int limit, Multiset<String> set, List<Cell> row) {
		for (Cell s : row) {
			String[] words = s.getValue().split("[^A-Za-z0-9]+", limit + 1);

			if (limit > 0 && words.length > limit) {
				set.addAll(Arrays.asList(words).subList(0, limit));
			} else {
				Collections.addAll(set, words);
			}
		}
	}

	public String getCaption() {
		return caption;
	}

	public String getHeadings() {
		return headings;
	}

	public boolean isEmpty() {
		for (List<Cell> row : rows)
			for (Cell s : row)
				if (!s.getValue().isEmpty())
					return false;
		return true;
	}

	private Integer hash = null;

	@Override
	public int hashCode() {
		if(hash != null) return hash;
		
		final int prime = 31;
		int result = 1;
		result = prime * result + ((caption == null) ? 0 : caption.hashCode());
		result = prime * result + ((rows == null) ? 0 : rows.hashCode());
		hash = result;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WikiTable other = (WikiTable) obj;
		if (caption == null) {
			if (other.caption != null)
				return false;
		} else if (!caption.equals(other.caption))
			return false;
		if (rows == null) {
			if (other.rows != null)
				return false;
		} else if (!rows.equals(other.rows))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TableObject [caption=" + caption + ", rows=" + rows + "]";
	}

	public int getRowCount() {
		return rows.size();
	}

	public List<Row> getRows() {
		List<Row> rows = new ArrayList<>();
		for (List<Cell> r : this.rows) {
			rows.add(new Row(r, false));
		}
		return rows;
	}
	
	private List<Row> columnsR = null;

	public List<Row> getColumns() {
		if(columnsR == null) {
			List<List<Cell>> columns = parseRowsToColumns();
	
			columnsR = new ArrayList<>();
			for (List<Cell> r : columns) {
				columnsR.add(new Row(r, true));
			}
		}
		return columnsR;
	}

	private List<List<Cell>> parseRowsToColumns() {
		List<List<Cell>> columns = new ArrayList<>();
		for (int row = 0; row < rows.size(); ++row) {
			List<Cell> rowData = rows.get(row);
			for (int column = 0; column < rowData.size(); ++column) {
				if (column >= columns.size()) {
					columns.add(new ArrayList<>());
				}
				while (columns.get(column).size() < row)
					columns.get(column).add(new Cell(""));

				columns.get(column).add(rowData.get(column));
			}
		}
		return columns;
	}

	public WtNode getNode() {
		return node;
	}

	public void setNode(WtNode n) {
		this.node = n;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}
