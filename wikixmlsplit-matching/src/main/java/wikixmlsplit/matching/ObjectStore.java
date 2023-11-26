package wikixmlsplit.matching;

import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.position.Position;
import wikixmlsplit.matching.ranking.ActiveTimeRanking;
import wikixmlsplit.matching.ranking.PositionRanking;
import wikixmlsplit.matching.ranking.SimilarityRankingPositionRestricted;
import wikixmlsplit.matching.similarity.SimilarityMeasure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ObjectStore<T> {



	private List<TrackedObject<T>> trackedObjects = null;

	private List<Matcher<T>> matchers;
	private long matchTime = 0;

	@SafeVarargs
	public ObjectStore(Matcher<T>... matchers) {
		this(Arrays.asList(matchers));
	}
	
	public ObjectStore(List<Matcher<T>> matchers) {
		this.matchers = new ArrayList<>(matchers);
		reset();
	}

	public void reset() {
		this.trackedObjects = new ArrayList<>();
	}
	
	public long getMatchTime() {
		return matchTime;
	}
	
	public void handleNewRevision(List<T> newObjects, Consumer<TrackedObject<T>> consumer, RevisionData r, BiFunction<T, Integer, ? extends Position> positionFct) {
		List<ObjectOberservation<T>> newObjectsObs = new ArrayList<>(newObjects.size());
		for(int i = 0; i < newObjects.size(); ++i) {
			newObjectsObs.add(new ObjectOberservation<>(newObjects.get(i), positionFct.apply(newObjects.get(i), i)));
		}
		handleNewRevision(newObjectsObs, consumer, r);
	}

	public void handleNewRevision(List<ObjectOberservation<T>> newObjects, Consumer<TrackedObject<T>> consumer, RevisionData r) {
		long start = System.nanoTime();
		if (newObjects == null || newObjects.isEmpty()) {
			markRemainingAsInactive(consumer, r);
			matchTime += System.nanoTime() - start;
			return;
		} else if (trackedObjects.isEmpty()) {
			// only new additions
			for (ObjectOberservation<T> entry : newObjects) {
				TrackedObject<T> newT = new TrackedObject<>(entry.getObject(), r, entry.getPosition());
				trackedObjects.add(newT);
				consumer.accept(newT);
			}
			matchTime += System.nanoTime() - start;
			return;
		}

		List<ObjectOberservation<T>> newObservations = createNewObservations(newObjects);

		// need to do a matching here
		// Use the different measures to compute matches. If matches fit, inform consumer
		match(consumer, r, newObservations);

		// new objects that did not match any previous existing objects
		// Handle the tables that found no match
		handleAdditions(consumer, r, newObservations);

		// previous existing objects that did not match any new objects
		markRemainingAsInactive(consumer, r);
		
		matchTime += System.nanoTime() - start;
	}

	private List<ObjectOberservation<T>> createNewObservations(List<ObjectOberservation<T>> newObjects) {
		List<ObjectOberservation<T>> newObservations = new ArrayList<>(newObjects.size());
		for(ObjectOberservation<T> entry : newObjects) {
			newObservations.add(new ObjectOberservation<>(entry.getObject(), entry.getPosition()));
		}
		return newObservations;
	}

	private void match(Consumer<TrackedObject<T>> consumer, RevisionData r,
			List<ObjectOberservation<T>> newObservations) {

		for(Matcher<T> m : matchers) {
			m.match(r, trackedObjects, newObservations, match -> {
				TrackedObject<T> tObj = match.getPrevious();
				tObj.update(match.getCurrent().getObject(), r, match.getCurrent().getPosition());
				consumer.accept(tObj);
				match.getCurrent().markMatched();
			});
		}
	}

	private void handleAdditions(Consumer<TrackedObject<T>> consumer, RevisionData r,
			List<ObjectOberservation<T>> newObservations) {
		for (ObjectOberservation<T> t : newObservations) {
			if (!t.isMatched()) {
				TrackedObject<T> newT = new TrackedObject<>(t.getObject(), r, t.getPosition());
				consumer.accept(newT);
				trackedObjects.add(newT);
			}
		}
	}

	private void markRemainingAsInactive(Consumer<TrackedObject<T>> consumer, RevisionData r) {
		for (TrackedObject<T> old : trackedObjects) {
			if (old.isActive() && old.getLastActiveRevision() != r) {
				old.setInactive(r);
				consumer.accept(old);
			}
		}
	}

	public int size() {
		return trackedObjects.size();
	}

	@Override
	public String toString() {
		return "OS [" + matchers.stream().map(Matcher::toString).collect(Collectors.joining("|")) + "]";
	}


	public static <S> ObjectStore<S> createDefault(SimilarityMeasure<? super S> sim, SimilarityMeasure<? super S> sim2, double limit1, double limit2, double limit3, double relaxLimit,
												   boolean greedy) {
		return createDefault(sim, sim2, limit1, limit2, limit3, relaxLimit, greedy, false);
	}
	public static <S> ObjectStore<S> createDefault(SimilarityMeasure<? super S> sim, SimilarityMeasure<? super S> sim2, double limit1, double limit2, double limit3, double relaxLimit,
													boolean greedy, boolean includeHash) {

		List<Matcher<S>> matchers = new ArrayList<>();

		if(includeHash) {
			matchers.add(new MatcherHash<>());
		}

		if (limit1 <= 1.0d)
			matchers.add(new MatcherBuilder<S>()
					.addRanking(new SimilarityRankingPositionRestricted<>(sim, limit1, relaxLimit, 2))
					.addRanking(new ActiveTimeRanking<>()).addRanking(new PositionRanking<>()).createMatcher(greedy));
		if (limit2 <= 1.0d)
			matchers.add(MatcherBuilder.createDefaultSimMatcher(sim, limit2, relaxLimit, greedy));
		if (limit3 <= 1.0d)
			matchers.add(MatcherBuilder.createDefaultSimMatcher(sim2, limit3, relaxLimit, greedy));
		return new ObjectStore<>(matchers);
	}
}
