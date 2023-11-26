package wikixmlsplit.matching.ranking;

import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.similarity.SimilarityMeasure;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;

public class SimilarityRanking<T> implements Ranking<T, Double> {


	protected final SimilarityMeasure<? super T> measure;
	protected final double simLimit;
	protected final double relaxLimit;
	
	public SimilarityRanking(SimilarityMeasure<? super T> measure, double simLimit, double relaxLimit) {
		this.measure = measure;
		this.simLimit = simLimit;
		this.relaxLimit = relaxLimit;
	}
	
	@Override
	public void register(Collection<Collection<T>> pastObjects, Collection<T> next) {
		measure.register(pastObjects, next);
	}

	@Override
	public Double getValue(TrackedObject<T> old, ObjectOberservation<T> current, RevisionData r) {
		double maxSim = 0;
		for (T s : old.getPastObjects()) {
			maxSim = Math.max(maxSim * 0.95d, measure.getSimilarity(s, current.getObject()));
		}
		return maxSim >= simLimit ? maxSim : null;
	}

	@Override
	public Comparator<Double> getComparator() {
		return (i,j) -> Double.compare(j, i);
	}

	@Override
	public Predicate<Double> getRelaxor(Double best) {
		return v -> v >= best * relaxLimit;
	}
	
	@Override
	public String toString() {
		return "SimilarityRanking [measure=" + measure + "| simLimit=" + simLimit + "| relaxLimit=" + relaxLimit + "]";
	}
}
