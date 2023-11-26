package wikixmlsplit.wikitable;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import wikixmlsplit.matching.similarity.BagStore;

import java.util.Collections;
import java.util.Map;

public class Cell {
	private String value;
	private Map<String, Object> properties;
	private Multiset<String> bagOfWords;
	
	public Cell(String value) {
		this(value, null);
	}

	public Cell(String value, Map<String, Object> properties) {
		super();
		this.value = value;
		this.properties = properties;
	}
	
	public String getValue() {
		return value;
	}
	
	public Object getProperty(String key) {
		return properties != null ? properties.get(key) : null;
	}
	
	private static BagStore<Multiset<String>> sets = new BagStore<>();
	public Multiset<String> getBagOfValues() {
		if(bagOfWords == null) {
			Multiset<String> set = HashMultiset.create();
			String[] words = value.split("[^A-Za-z0-9]+");
			Collections.addAll(set, words);
			set.remove("", set.count(""));
			this.bagOfWords = sets.get(set);
		}
		return bagOfWords;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Cell other = (Cell) obj;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Cell [value=" + value + ", properties=" + properties + "]";
	}


}
