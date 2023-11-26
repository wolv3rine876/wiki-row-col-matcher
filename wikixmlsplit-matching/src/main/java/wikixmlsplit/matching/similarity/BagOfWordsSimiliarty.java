package wikixmlsplit.matching.similarity;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import java.util.regex.Pattern;

public class BagOfWordsSimiliarty implements SimilarityMeasure<Multiset<String>> {

	private static final Pattern numbers = Pattern.compile("[0-9]+");

	private final boolean punishSizeChange;
	private final double numberWeight;

	public BagOfWordsSimiliarty(boolean punishSizeChange) {
		this(punishSizeChange, false);
	}

	public BagOfWordsSimiliarty(boolean punishSizeChange, boolean lowerNumberWeight) {
		this(punishSizeChange, lowerNumberWeight ? 0.3d : 1.0d);
	}
	
	
	public BagOfWordsSimiliarty(boolean punishSizeChange, double numberWeight) {
		this.punishSizeChange = punishSizeChange;
		this.numberWeight = numberWeight;
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
			double factor = getWeight(s);

			double c1 = s.getCount();
			double c2 = bag2.count(s.getElement());
			numer += Math.min(c1, c2) * factor;
			denom += c1 * factor;
			denom2 += c2 * factor;
			denomMax += Math.max(c1, c2) * factor;
		}

		for (Entry<String> s : bag2.entrySet()) {
			if (s.getElement().isEmpty() || bag1.contains(s.getElement()))
				continue;
			double factor = getWeight(s);
			double c2 = s.getCount();
			denom2 += c2 * factor;
			denomMax += c2 * factor;
		}

		denom = punishSizeChange ? denomMax : Math.min(denom, denom2);

		if (denom == 0)
			return 1.0d;

		return numer / denom;
	}

	private double getWeight(Entry<String> s) {
		return numberWeight != 1.0d && numbers.matcher(s.getElement()).matches() ? numberWeight : 1.0d;
	}

}
