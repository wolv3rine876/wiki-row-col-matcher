package wikixmlsplit.matching.ranking;

import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;

import java.util.Comparator;
import java.util.function.Predicate;

public class PositionRanking<T> implements Ranking<T, Integer> {



	@Override
	public Integer getValue(TrackedObject<T> old, ObjectOberservation<T> current, RevisionData r) {
		return old.getCurrentPosition().getDifference(current.getPosition());
	}

	public Comparator<Integer> getComparator() {
		return Integer::compare;
	}

	@Override
	public Predicate<Integer> getRelaxor(Integer best) {
		return integer -> false;
	}
	
	@Override
	public String toString() {
		return "PositionRanking []";
	}
}
