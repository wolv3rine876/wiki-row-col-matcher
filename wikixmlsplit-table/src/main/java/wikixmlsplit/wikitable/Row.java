package wikixmlsplit.wikitable;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import wikixmlsplit.matching.similarity.BagStore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Row {

	private List<Cell> values;
	private boolean vertical;

	public Row(List<Cell> values, boolean vertical) {
		this.values = values;
		this.vertical = vertical;
	}

	public int getSize() {
		return values.size();
	}

	public String getValue(int position) {
		return values.get(position).getValue();
	}

	public List<String> getValues() {
		return values.stream().map(Cell::getValue).collect(Collectors.toList());
	}

	public double getSimilarity(Row other, BiMap<Integer, Integer> fixed) {
		Multiset<String> thisMultiset = HashMultiset.create();
		Multiset<String> otherMultiset = HashMultiset.create();

		int numer = 0;
		int denom = 0;
		for (int i = 0; i < this.getSize(); ++i) {
			if (fixed.containsKey(i)) {
				int otherIndex = fixed.get(i);
				if (other.getSize() > otherIndex && other.getValue(otherIndex).equals(values.get(i).getValue()))
					++numer;
				++denom;
			} else {
				thisMultiset.add(values.get(i).getValue());
			}
		}
		for (int i = 0; i < other.getSize(); ++i) {
			if (!fixed.containsValue(i)) {
				otherMultiset.add(other.getValue(i));
			}
		}

		Set<String> valueUnion = new HashSet<>();
		valueUnion.addAll(thisMultiset.elementSet());
		valueUnion.addAll(otherMultiset.elementSet());

		for (String s : valueUnion) {
			if (s.isEmpty())
				continue;
			int c1 = thisMultiset.count(s);
			int c2 = otherMultiset.count(s);
			numer += Math.min(c1, c2);
			denom += Math.max(c1, c2);
		}

		return ((double) numer) / denom;
	}

	private Multiset<String> wordSet = null;
	private static BagStore<Multiset<String>> sets = new BagStore<>();

	public Multiset<String> getBagOfValues() {
		if (wordSet != null)
			return wordSet;

		Multiset<String> thisMultiset = HashMultiset.create();
		for (Cell s : values) {
			Multiset<String> bow = s.getBagOfValues();
			for (int i = 0; i < (s.getProperty("header") != null ? 5 : 1); ++i)
				thisMultiset = Multisets.union(thisMultiset, bow);
		}
		thisMultiset = HashMultiset.create(thisMultiset);
		this.wordSet = sets.get(thisMultiset);

		return wordSet;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		result = prime * result + (vertical ? 1231 : 1237);
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
		Row other = (Row) obj;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		if (vertical != other.vertical)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Row [values=" + values + ", vertical=" + vertical + "]";
	}
}
