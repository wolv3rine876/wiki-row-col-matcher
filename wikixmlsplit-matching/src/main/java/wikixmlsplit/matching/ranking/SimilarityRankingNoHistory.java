package wikixmlsplit.matching.ranking;

import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.similarity.SimilarityMeasure;

public class SimilarityRankingNoHistory<T> extends SimilarityRanking<T> {


	public SimilarityRankingNoHistory(SimilarityMeasure<T> measure, double simLimit, double relaxLimit) {
		super(measure, simLimit, relaxLimit);
	}
	

	@Override
	public Double getValue(TrackedObject<T> old, ObjectOberservation<T> current, RevisionData r) {
		double maxSim = 0;
		for (T s : old.getPastObjects()) {
			maxSim = Math.max(maxSim * 0.95d, measure.getSimilarity(s, current.getObject()));
			break;
		}
		return maxSim >= simLimit ? maxSim : null;
	}

	
	@Override
	public String toString() {
		return "SimilarityRankingNoHistory [measure=" + measure + "| simLimit=" + simLimit + "| relaxLimit=" + relaxLimit + "]";
	}
}
