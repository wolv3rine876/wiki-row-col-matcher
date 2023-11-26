package wikixmlsplit.matching.ranking;

import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

public interface Ranking<T, V> {
	V getValue(TrackedObject<T> old, ObjectOberservation<T> current, RevisionData r);

	Comparator<V> getComparator();

	Predicate<V> getRelaxor(V best);

	default void register(Collection<Collection<T>> pastObjects, Collection<T> next) {

	}

}
