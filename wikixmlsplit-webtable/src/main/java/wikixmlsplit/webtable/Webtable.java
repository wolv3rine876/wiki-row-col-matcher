package wikixmlsplit.webtable;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import wikixmlsplit.matching.similarity.BagStore;
import wikixmlsplit.matching.similarity.SchemaObject;
import wikixmlsplit.webtable.io.Util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Webtable implements SchemaObject {

	private String caption;
	private List<List<String>> rows;
	private String headings;

	private Element element;
	private Multiset<String> bagOfWords = null;

	private Multiset<String> bagOfWordsFirstLine = null;

	public Webtable(Element element, String[] header) {
		this.element = element;

		String head = "";
		for (String s : header) {
			if (s != null) {
				if (!head.isEmpty())
					head = head.concat("|");
				head = head.concat(s);
			}
		}
		this.headings = head;
		Elements caption = element.select("caption");
		if (caption.size() == 1) {
			this.caption = Util.cleanCell(caption.get(0).text());
		}

		this.rows = parseTable(element);
	}

	public Element getElement() {
		return element;
	}

	public String getCaption() {
		return caption;
	}

	public List<List<String>> getRows() {
		return rows;
	}

	public String getHeadings() {
		return headings;
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
			for (List<String> row : rows) {
				for (String s : row) {
					String[] words = s.split("[^A-Za-z0-9]+", limit + 1);
					if (limit > 0 && words.length > limit) {
						set.addAll(Arrays.asList(words).subList(0, limit));
					} else {
						Collections.addAll(set, words);
					}
				}
			}
			set.remove("", set.count(""));
			bagOfWords = sets.get(set);
		}

		return bagOfWords;
	}
	

	public Multiset<String> getSchemaBoW(int limit) {
		if (bagOfWordsFirstLine == null) {
			Multiset<String> set = HashMultiset.create();
			for (List<String> row : rows) {
				for (String s : row) {

					String[] words = s.split("[^A-Za-z0-9]+", limit + 1);

					if (limit > 0 && words.length > limit) {
						set.addAll(Arrays.asList(words).subList(0, limit));
					} else {
						Collections.addAll(set, words);
					}
				}
				break;
			}
			set.remove("", set.count(""));
			bagOfWordsFirstLine = sets.get(set);
		}

		return bagOfWordsFirstLine;
	}

	List<List<String>> parseTable(Element e) {
		return e.select("tr").stream()
				// Select all <td> tags in single row
				.map(tr -> tr.select("td, th"))
				// Repeat n-times those <td> that have `colspan="n"` attribute
				.map(rows -> rows.stream()
						.map(td -> Collections.nCopies(td.hasAttr("colspan") ? Integer.parseInt(td.attr("colspan")) : 1,
								td))
						.flatMap(Collection::stream).collect(Collectors.toList()))
				// Fold final structure to 2D List<List<Element>>
				.reduce(new ArrayList<List<Element>>(), (acc, row) -> {
					// First iteration - just add current row to a final structure
					if (acc.isEmpty()) {
						acc.add(row);
						return acc;
					}

					// If last array in 2D array does not contain element with `rowspan` - append
					// current
					// row and skip to next iteration step
					final List<Element> last = acc.get(acc.size() - 1);
					if (last.stream().noneMatch(td -> td.hasAttr("rowspan"))) {
						acc.add(row);
						return acc;
					}

					// In this case last array in 2D array contains an element with `rowspan` - we
					// are going to
					// add this element n-times to current rows where n == rowspan - 1
					final AtomicInteger index = new AtomicInteger(0);
					last.stream()
							// Map to a helper list of (index in array, rowspan value or 0 if not present,
							// Jsoup element)
							.map(td -> Arrays.asList(0,
									Integer.valueOf(td.hasAttr("rowspan") ? td.attr("rowspan") : "0"), td))
							// Filter out all elements without rowspan
							.filter(it -> ((int) it.get(1)) > 1)
							// Add all elements with rowspan to current row at the index they are present
							// (add them with `rowspan="n-1"`)
							.forEach(it -> {
								final int idx = index.getAndIncrement();
								final int rowspan = (int) it.get(1);
								final Element td = (Element) it.get(2);

								row.add(idx, rowspan - 1 == 0 ? (Element) td.removeAttr("rowspan")
										: td.attr("rowspan", String.valueOf(rowspan - 1)));
							});

					acc.add(row);
					return acc;
				}, (a, b) -> a).stream()
				// Extract inner HTML text from Jsoup elements in 2D array
				.map(tr -> tr.stream().map(Element::text).collect(Collectors.toList())).collect(Collectors.toList());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((caption == null) ? 0 : caption.hashCode());
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
		Webtable other = (Webtable) obj;
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

	public List<List<String>> getColumns() {
		List<List<String>> columns = new ArrayList<>();
		
		for(List<String> row : rows) {
			for(int i = 0; i < row.size(); ++i) {
				if(i >= columns.size()) {
					columns.add(new ArrayList<>());
				}
				List<String> column = columns.get(i);
				column.add(row.get(i));
			}
		}
		return columns;
	}

	private String filename;
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
}
