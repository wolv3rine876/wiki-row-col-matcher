package wikixmlsplit.matching.ranking;

import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.similarity.SimilarityMeasure;

public class SimilarityRankingPositionRestricted<T> extends SimilarityRanking<T> {
	private final int maxPositionDifference;
	
	public SimilarityRankingPositionRestricted(SimilarityMeasure<? super T> measure, double simLimit, double relaxLimit, int maxPositionDifference) {
		super(measure, simLimit, relaxLimit);
		this.maxPositionDifference = maxPositionDifference;
	}
	
	@Override
	public Double getValue(TrackedObject<T> old, ObjectOberservation<T> current, RevisionData r) {
		if(old.getCurrentPosition().getDifference(current.getPosition()) > maxPositionDifference)
			return null;

		return super.getValue(old, current, r);
	}

}
