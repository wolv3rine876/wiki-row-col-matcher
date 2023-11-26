package wikixmlsplit.matching.ranking;

import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;

import java.util.Comparator;
import java.util.function.Predicate;

public class ActiveTimeRanking<T> implements Ranking<T, Long> {


	@Override
	public Long getValue(TrackedObject<T> old, ObjectOberservation<T> current, RevisionData r) {
		return old.getActiveTime(r);
	}

	@Override
	public Comparator<Long> getComparator() {
		return (i, j) -> Long.compare(j, i);
	}

	@Override
	public Predicate<Long> getRelaxor(Long best) {
		return v -> v >= best * 0.5d;
	}

	@Override
	public String toString() {
		return "ActiveTimeRanking []";
	}

}
