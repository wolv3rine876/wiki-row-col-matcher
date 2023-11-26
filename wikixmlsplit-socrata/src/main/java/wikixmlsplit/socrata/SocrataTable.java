package wikixmlsplit.socrata;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import wikixmlsplit.matching.similarity.BagStore;
import wikixmlsplit.matching.similarity.SchemaObject;
import wikixmlsplit.socrata.input.MetaData;

import java.util.*;

public class SocrataTable implements SchemaObject {

	private List<String> header;
	private List<List<String>> rows;
	private MetaData meta;

	public SocrataTable(List<String> header, List<List<String>> rows, MetaData meta) {
		this.header = header;
		this.rows = rows;

		this.meta = meta;
	}

	public List<String> getHeader() {
		return header;
	}

	public List<List<String>> getRows() {
		return rows;
	}

	public boolean isUnique(int column) {
		Set<String> set = new HashSet<>(rows.size());
		for (List<String> row : rows) {
			if (!set.add(row.get(column))) {
				return false;
			}
		}
		return true;
	}
	
	public int getUniqueCount(int column) {
		Set<String> set = new HashSet<>(rows.size());
		for (List<String> row : rows) {
			set.add(row.get(column));
		}
		return set.size();
	}

	public List<List<String>> getHeaderAndRows() {
		List<List<String>> result = new ArrayList<>();
		result.add(header);
		result.addAll(rows);
		return result;
	}

	public boolean contentEquals(SocrataTable other) {
		return header.equals(other.header) && rows.equals(other.rows);
	}

	private static BagStore<Multiset<String>> sets = new BagStore<>();
	private Multiset<String> bagOfWords = null;

	public Multiset<String> getBagOfValues(int limit, boolean includeHeaders) {
		if (bagOfWords == null) {
			computeBoW(limit);
		}

		return bagOfWords;
	}

	protected void computeBoW(int limit) {
		Multiset<String> set = HashMultiset.create();
		for (String s : header) {
			add(limit, set, s);
		}

		for (List<String> row : rows) {
			for (String s : row) {
				add(limit, set, s);
			}
		}

		if (meta != null) {
			if (meta.resource.name != null)
				Collections.addAll(set, meta.resource.name.split("[^A-Za-z0-9]+"));
			if (meta.resource.description != null)
				Collections.addAll(set, meta.resource.description.split("[^A-Za-z0-9]+"));
		}

		set.remove("", set.count(""));
		bagOfWords = sets.get(set);
	}

	protected void add(int limit, Multiset<String> set, String s) {
		String[] words = s.split("[^A-Za-z0-9]+", limit + 1);
		if (limit > 0 && words.length > limit) {
			set.addAll(Arrays.asList(words).subList(0, limit));
		} else {
			Collections.addAll(set, words);
		}
	}

	@Override
	public String toString() {
		return "SocrataTable [rows=" + rows + ", bagOfWords=" + bagOfWords + "]";
	}

	public String toHtml(int rowLimit) {
		StringBuilder builder = new StringBuilder();
		builder.append("<table>");

		if (meta != null) {
			String name = meta.resource.name != null ? meta.resource.name : "";
			String description = meta.resource.description != null ? meta.resource.description : "";

			builder.append("<caption>").append(name).append(" {").append(description).append("}</caption>");
		}
		builder.append("<tr>");
		for (String cell : header) {
			builder.append("<th>").append(cell).append("</th>");
		}
		builder.append("</tr>");

		int rowCount = 0;
		for (List<String> row : rows) {
			builder.append("<tr>");
			for (String cell : row) {
				builder.append("<td>").append(cell).append("</td>");
			}
			builder.append("</tr>");
			if (++rowCount == rowLimit)
				break;
		}
		builder.append("</table>");
		return builder.toString();
	}

	public boolean isEmpty() {
		return rows.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((header == null) ? 0 : header.hashCode());
		result = prime * result + ((meta == null) ? 0 : meta.hashCode());
		result = prime * result + ((rows == null) ? 0 : rows.hashCode());
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
		SocrataTable other = (SocrataTable) obj;
		if (header == null) {
			if (other.header != null)
				return false;
		} else if (!header.equals(other.header))
			return false;
		if (meta == null) {
			if (other.meta != null)
				return false;
		} else if (!meta.equals(other.meta))
			return false;
		if (rows == null) {
			if (other.rows != null)
				return false;
		} else if (!rows.equals(other.rows))
			return false;
		return true;
	}

	private Multiset<String> bagOfWordsHeader = null;

	public Multiset<String> getSchemaBoW(int limit) {
		if(bagOfWordsHeader != null)
			return bagOfWordsHeader;
		
		Multiset<String> set = HashMultiset.create();
		for (String s : header) {
			add(limit, set, s);
		}

		set.remove("", set.count(""));
		bagOfWordsHeader = sets.get(set);
		return bagOfWordsHeader;
	}

	List<List<String>> columns = null;
	public List<List<String>> getColumns() {
		if(columns != null)
			return columns;
		
		columns = new ArrayList<>(header.size());
		for (int row = 0; row < rows.size(); ++row) {
			List<String> rowData = rows.get(row);
			for (int column = 0; column < rowData.size(); ++column) {
				if (column >= columns.size()) {
					columns.add(new ArrayList<>());
				}
				while (columns.get(column).size() < row)
					columns.get(column).add("");

				columns.get(column).add(rowData.get(column));
			}
		}
		
		return columns;
	}

	public MetaData getMeta() {
		return meta;
	}

}
