package wikixmlsplit.matching.similarity;

import com.google.common.collect.Multiset;

public class SchemaObjectSchemaBoWSimilarity implements SimilarityMeasure<SchemaObject> {

	protected static final int WORD_COUNT = 10;
	protected final boolean punishSizeChange;
	protected SimilarityMeasure<Multiset<String>> sim;

	public SchemaObjectSchemaBoWSimilarity(boolean punishSizeChange) {
		this(punishSizeChange, new CachingSimilarity<>(new BagOfWordsSimiliarty(punishSizeChange)));
	}
	
	protected SchemaObjectSchemaBoWSimilarity(boolean punishSizeChange, SimilarityMeasure<Multiset<String>> sim) {
		this.sim = sim;
		this.punishSizeChange = punishSizeChange;
	}

	@Override
	public double getSimilarity(SchemaObject object1, SchemaObject object2) {
		Multiset<String> bag1 = object1.getSchemaBoW(WORD_COUNT);
		Multiset<String> bag2 = object2.getSchemaBoW(WORD_COUNT);

		return sim.getSimilarity(bag1, bag2);
	}

	@Override
	public String toString() {
		return "SchemaObjectSchemaBoWSimilarity [" + punishSizeChange + "]";
	}


}
