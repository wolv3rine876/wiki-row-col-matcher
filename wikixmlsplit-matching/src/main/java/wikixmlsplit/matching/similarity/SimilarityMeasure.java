package wikixmlsplit.matching.similarity;

import java.util.Collection;

public interface SimilarityMeasure<T> {
	double getSimilarity(T object1, T object2);

	default void register(Collection<? extends Collection<? extends T>> pastObjects, Collection<? extends T> newObjects) {
		
	}
}
