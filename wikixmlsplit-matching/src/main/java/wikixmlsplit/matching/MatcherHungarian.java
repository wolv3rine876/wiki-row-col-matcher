package wikixmlsplit.matching;

import wikixmlsplit.matching.data.MatchPair;
import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.ranking.Ranking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MatcherHungarian<T> implements Matcher<T> {

	private final List<Ranking<T, ?>> rankings;
	private boolean disablePosition;

	MatcherHungarian(List<Ranking<T, ?>> rankings, boolean disablePosition) {
		this.rankings = new ArrayList<>(rankings);
		this.disablePosition = disablePosition;
	}

	@Override
	public void match(RevisionData r, List<TrackedObject<T>> trackedObjects,
			List<ObjectOberservation<T>> newObservations, Consumer<MatchPair<T>> consumer) {


		Collection<Collection<T>> previousObjects = trackedObjects.stream().filter(previous -> previous.getLastActiveRevision() != r).map(TrackedObject::getPastObjects).collect(Collectors.toList());
		Collection<T> newObjects = newObservations.stream().filter(((Predicate<ObjectOberservation<T>>) ObjectOberservation::isMatched).negate()).map(ObjectOberservation::getObject).collect(Collectors.toList());
		rankings.get(0).register(previousObjects, newObjects);

		double[][] costs = new double[trackedObjects.size()][newObservations.size()];

		List<Double> costList = new ArrayList<>();
		int maxPosDifference = 1;
		// calculate similarities
		for (int oldId = 0; oldId < trackedObjects.size(); ++oldId) {
			TrackedObject<T> old = trackedObjects.get(oldId);
			if (old.getLastActiveRevision() == r)
				continue;

			for (int newId = 0; newId < newObservations.size(); ++newId) {
				ObjectOberservation<T> current = newObservations.get(newId);
				if (current.isMatched())
					continue;
				Object value = rankings.get(0).getValue(old, current, r);
				if (value != null) {
					costs[oldId][newId] = -(double) value;
					costList.add((double) value);
					maxPosDifference = Math.max(maxPosDifference,
							old.getCurrentPosition().getDifference(current.getPosition()));
				}
			}
		}
		Collections.sort(costList);

		// get smallest gap between similarities
		double minDiff = 1E-6;
		for (int i = 0; i < costList.size() - 1; ++i) {
			double diff = costList.get(i + 1) - costList.get(i);
			if (diff > 0 && diff < minDiff) {
				minDiff = diff;
			}
		}

		// get maximum life time
		long maxLifeTime = 1;
		for (TrackedObject<T> trackedObject : trackedObjects) {
			if (trackedObject.getLastActiveRevision() == r)
				continue;
			maxLifeTime = Math.max(maxLifeTime, trackedObject.getActiveTime(r));
		}

		for (int oldId = 0; oldId < trackedObjects.size(); ++oldId) {
			TrackedObject<T> old = trackedObjects.get(oldId);
			if (old.getLastActiveRevision() == r)
				continue;

			double tieBreaker = (minDiff * (double) old.getActiveTime(r)) / maxLifeTime;
			for (int newId = 0; newId < newObservations.size(); ++newId) {
				ObjectOberservation<T> current = newObservations.get(newId);
				if (current.isMatched())
					continue;
				if (costs[oldId][newId] < 0) {
					costs[oldId][newId] -= tieBreaker;
					if (!disablePosition) {
						int posDiff = old.getCurrentPosition().getDifference(current.getPosition());
						costs[oldId][newId] -= (minDiff * (maxPosDifference - posDiff)) / maxPosDifference;
					}
				}
			}
		}

		HungarianAlgorithm algo = new HungarianAlgorithm(costs);
		int[] assignment = algo.execute();
		for (int oldId = 0; oldId < trackedObjects.size(); ++oldId) {
			int newId = assignment[oldId];
			if (newId < 0)
				continue;

			if (costs[oldId][newId] >= 0)
				continue;

			consumer.accept(new MatchPair<>(trackedObjects.get(oldId), newObservations.get(newId)));
		}

	}

}
