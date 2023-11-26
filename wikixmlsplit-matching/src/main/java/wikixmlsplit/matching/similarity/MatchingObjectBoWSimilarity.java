package wikixmlsplit.matching.similarity;

import com.google.common.collect.Multiset;

public class MatchingObjectBoWSimilarity implements SimilarityMeasure<MatchingObject> {

	protected static final int WORD_COUNT = 10;
	protected final boolean punishSizeChange;
	protected SimilarityMeasure<Multiset<String>> sim;

	public MatchingObjectBoWSimilarity(boolean punishSizeChange) {
		this(punishSizeChange, new CachingSimilarity<>(new BagOfWordsSimiliarty(punishSizeChange)));
	}

	protected MatchingObjectBoWSimilarity(boolean punishSizeChange, SimilarityMeasure<Multiset<String>> sim) {
		this.sim = sim;
		this.punishSizeChange = punishSizeChange;
	}

	public double getSimilarity(MatchingObject t1, MatchingObject t2) {
		Multiset<String> bag1 = t1.getBagOfValues(WORD_COUNT, true);
		Multiset<String> bag2 = t2.getBagOfValues(WORD_COUNT, true);

		return sim.getSimilarity(bag1, bag2);
	}

	@Override
	public String toString() {
		return "MatchingObjectBoWSimilarity [" + punishSizeChange + "]";
	}

}
