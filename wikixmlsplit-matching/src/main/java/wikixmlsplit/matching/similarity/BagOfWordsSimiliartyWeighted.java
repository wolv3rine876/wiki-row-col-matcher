package wikixmlsplit.matching.similarity;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

public class BagOfWordsSimiliartyWeighted implements SimilarityMeasure<Multiset<String>> {


	private final Multiset<String> counts;
	private final boolean punishSizeChange;

	public BagOfWordsSimiliartyWeighted(boolean punishSizeChange, Multiset<String> counts) {
		this.counts = counts;
		this.punishSizeChange = punishSizeChange;
	}

	@Override
	public double getSimilarity(Multiset<String> bag1, Multiset<String> bag2) {
		if (bag1.isEmpty() && bag2.isEmpty())
			return 1.0d;

		double numer = 0;
		double denom = 0;
		double denom2 = 0;
		double denomMax = 0.0d;
		for (Entry<String> s : bag1.entrySet()) {
			if (s.getElement().isEmpty())
				continue;
			double factor = Math.max(counts.count(s.getElement()), 1);

			double c1 = s.getCount();
			double c2 = bag2.count(s.getElement());
			numer += Math.min(c1, c2) / factor;
			denom += c1 / factor;
			denom2 += c2 / factor;
			denomMax += Math.max(c1, c2) / factor;
		}

		for (Entry<String> s : bag2.entrySet()) {
			if (s.getElement().isEmpty() || bag1.contains(s.getElement()))
				continue;
			double factor = Math.max(counts.count(s.getElement()), 1);
			denom2 += ((double)s.getCount()) / factor;
			denomMax +=((double)s.getCount()) / factor;
		}

		denom = punishSizeChange ? denomMax: Math.min(denom, denom2);

		if (denom == 0)
			return 1.0d;

		return numer / denom;
	}

}
