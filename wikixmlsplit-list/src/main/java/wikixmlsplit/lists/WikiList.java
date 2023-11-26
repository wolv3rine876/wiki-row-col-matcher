package wikixmlsplit.lists;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.sweble.wikitext.parser.nodes.WtNode;
import wikixmlsplit.matching.similarity.BagStore;
import wikixmlsplit.matching.similarity.MatchingObject;
import wikixmlsplit.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WikiList implements MatchingObject {

	private List<String> items = new ArrayList<>();
	private String headings;
	private WtNode node;

	private Multiset<String> bagOfWords = null;

	private static BagStore<Multiset<String>> sets = new BagStore<>();

	public Multiset<String> getBagOfValues(int limit, boolean includeHeaders) {
		if (bagOfWords == null) {
			Multiset<String> set = HashMultiset.create();

			for (String value : items) {
				Util.addWords(limit, set, value);
			}
			
			if(includeHeaders) {
				Util.addWords(limit, set, headings);
			}

			set.remove("", set.count(""));
			bagOfWords = sets.get(set);
		}

		return bagOfWords;
	}


	public String getHeadings() {
		return headings;
	}

	public void setHeadings(String headings) {
		this.headings = headings;
	}

	public WtNode getNode() {
		return node;
	}

	public void setNode(WtNode node) {
		this.node = node;
	}

	public List<String> getItems() {
		return Collections.unmodifiableList(items);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((headings == null) ? 0 : headings.hashCode());
		result = prime * result + ((items == null) ? 0 : items.hashCode());
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
		WikiList other = (WikiList) obj;
		if (headings == null) {
			if (other.headings != null)
				return false;
		} else if (!headings.equals(other.headings))
			return false;
		if (items == null) {
			if (other.items != null)
				return false;
		} else if (!items.equals(other.items))
			return false;
		return true;
	}

	public void addItem(String string) {
		this.items.add(string);
	}

	public int getItemCount() { return this.items.size(); }

	@Override
	public String toString() {
		return "WikiList [items=" + items + ", headings=" + headings + ", node=" + node + ", bagOfWords=" + bagOfWords
				+ "]";
	}
}
