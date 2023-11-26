package wikixmlsplit.matching.similarity;

import com.google.common.collect.Multiset;

public interface SchemaObject extends MatchingObject {

    Multiset<String> getSchemaBoW(int limit);
}
