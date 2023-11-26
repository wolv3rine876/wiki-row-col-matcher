package wikixmlsplit.matching.similarity;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Set;


public class MatchingObjectBoWSimilarityWeighted extends MatchingObjectBoWSimilarity {

	public MatchingObjectBoWSimilarityWeighted(boolean punishSizeChange) {
		super(punishSizeChange);
		this.sim = new CachingSimilarity<>(new BagOfWordsSimiliartyWeighted(punishSizeChange, HashMultiset.create()));
	}

	@Override
	public void register(Collection<? extends Collection<? extends MatchingObject>> pastObjects, Collection<? extends MatchingObject> newObjects) {

		Multiset<String> pastWeights = HashMultiset.create();
		for (Collection<? extends MatchingObject> tables : pastObjects) {
			Set<String> words = null;
			for (MatchingObject table : tables) {
				Multiset<String> bag = table.getBagOfValues(WORD_COUNT, true);
				if (words == null) {
					words = bag.elementSet();
				} else {
					words = Sets.union(words, bag.elementSet());
				}
			}
			if (words != null)
				pastWeights.addAll(words);
		}
		Multiset<String> newWeights = HashMultiset.create();
		for (MatchingObject table : newObjects) {
			Multiset<String> bag = table.getBagOfValues(WORD_COUNT, true);
			newWeights.addAll(bag.elementSet());
		}
		this.sim = new CachingSimilarity<>(
				new BagOfWordsSimiliartyWeighted(punishSizeChange, Multisets.union(pastWeights, newWeights)));
	}

	@Override
	public String toString() {
		return "TableBagOfValueSimilarityWeighted [" + punishSizeChange + "]";
	}

}
