package wikixmlsplit.infobox;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.sweble.wikitext.parser.nodes.WtNode;
import wikixmlsplit.matching.similarity.BagStore;
import wikixmlsplit.matching.similarity.SchemaObject;
import wikixmlsplit.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Infobox implements SchemaObject {

	private String template;
	private HashMap<String, String> attributes = new HashMap<>();
	private String headings;
	private WtNode node;

	public int getAttributeCount() {
		return attributes.size();
	}

	public int getSameValueCount(Infobox other) {
		int result = 0;
		for (Entry<String, String> i : attributes.entrySet()) {
			String otherValue = other.attributes.get(i.getKey());
			if (i.getValue().equals(otherValue))
				++result;
		}
		return result;
	}

	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	private List<String> serial = null;
	private static BagStore<List<String>> serialStore = new BagStore<>();

	private Multiset<String> bagOfWords = null;

	private static BagStore<Multiset<String>> sets = new BagStore<>();

	public Multiset<String> getBagOfValues(int limit, boolean includeHeaders) {
		if (bagOfWords == null) {
			Multiset<String> set = HashMultiset.create();

			for (String value : attributes.keySet()) {
				Util.addWords(limit, set, value);
			}
			
			for (String value : attributes.values()) {
				Util.addWords(limit, set, value);
			}
			
			Util.addWords(limit, set, template);
			
			if(includeHeaders) {
				Util.addWords(limit, set, headings);
			}

			set.remove("", set.count(""));
			bagOfWords = sets.get(set);
		}

		return bagOfWords;
	}

	@Override
	public Multiset<String> getSchemaBoW(int limit) {
		Multiset<String> bag1 = HashMultiset.create();
		bag1.addAll(getAttributes().keySet());
		return bag1;
	}

	public void setTemplate(String template) {
		this.template = template.trim().toLowerCase();
	}

	public void addAttribute(String name, String value) {
		attributes.put(name.trim().toLowerCase(), value.trim());
	}

	public String getTemplate() {
		return template;
	}

	public String getHeadings() {
		return headings;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((template == null) ? 0 : template.hashCode());
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
		Infobox other = (Infobox) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (template == null) {
			if (other.template != null)
				return false;
		} else if (!template.equals(other.template))
			return false;
		return true;
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

	@Override
	public String toString() {
		return "Infobox [template=" + template + ", attributes=" + attributes + ", headings=" + headings  + ", bagOfWords=" + bagOfWords + "]";
	}

}
