package wikixmlsplit.matching.similarity;

import com.google.common.collect.Multiset;

public interface MatchingObject {

	Multiset<String> getBagOfValues(int limit, boolean includeHeaders);
}
