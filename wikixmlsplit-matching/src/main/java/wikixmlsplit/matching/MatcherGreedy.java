package wikixmlsplit.matching;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import wikixmlsplit.matching.data.MatchPair;
import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.ranking.IntermediateRankingResult;
import wikixmlsplit.matching.ranking.Ranking;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MatcherGreedy<T> implements Matcher<T> {


	private final List<Ranking<T, ?>> rankings;

	MatcherGreedy(List<Ranking<T, ?>> rankings) {
		this.rankings = new ArrayList<>(rankings);
	}

	@Override
	public void match(RevisionData r, List<TrackedObject<T>> trackedObjects,
			List<ObjectOberservation<T>> newObservations, Consumer<MatchPair<T>> consumer) {
		Consumer<IntermediateRankingResult<T>> refinementFunction = input -> input.getBest(rankings.get(rankings.size() - 1), consumer);
		for (int i = rankings.size() - 2; i >= 1; --i) {
			Ranking<T, ?> currentRanking = rankings.get(i);
			final Consumer<IntermediateRankingResult<T>> ref = refinementFunction;
			refinementFunction = input -> input.refine(currentRanking, ref);
		}
		sort(rankings.get(0), trackedObjects, newObservations, r, refinementFunction);
	}
	
	private <V> void sort(Ranking<T,V> ranking, List<TrackedObject<T>> trackedObjects, List<ObjectOberservation<T>> newObservations,
			RevisionData r, Consumer<IntermediateRankingResult<T>> consumer) {
		List<MatchPair<T>> list = new ArrayList<>();
		Map<MatchPair<T>, V> values = new HashMap<>();
		createMatchPairs(ranking, trackedObjects, newObservations, r, list, values);

		list.sort(Comparator.comparing(values::get, ranking.getComparator()));

		matchWithinLimits(ranking, r, consumer, list, values);
	}

	private <V> void matchWithinLimits(Ranking<T, V> ranking, RevisionData r,
			Consumer<IntermediateRankingResult<T>> consumer, List<MatchPair<T>> list, Map<MatchPair<T>, V> values) {
		List<MatchPair<T>> active = new ArrayList<>();

		PeekingIterator<MatchPair<T>> iter = Iterators.peekingIterator(list.iterator());
		while (iter.hasNext() || !active.isEmpty()) {
			// get currently best similarity
			active = updateActiveList(r, active, iter);
			if (active.isEmpty())
				continue;
			MatchPair<T> best = active.get(0);

			// get all the candidates that are within a certain range of the currently best
			// match
			addCandidatesWithinLimit(ranking, r, values, active, iter, best);

			consumer.accept(new IntermediateRankingResult<>(r, active));
		}
	}

	private <V> void createMatchPairs(Ranking<T, V> ranking, List<TrackedObject<T>> trackedObjects,
			List<ObjectOberservation<T>> newObservations, RevisionData r, List<MatchPair<T>> list,
			Map<MatchPair<T>, V> values) {
		
		
		Collection<Collection<T>> previousObjects = trackedObjects.stream().filter(previous -> previous.getLastActiveRevision() != r).map(TrackedObject::getPastObjects).collect(Collectors.toList());
		Collection<T> newObjects = newObservations.stream().filter(((Predicate<ObjectOberservation<T>>) ObjectOberservation::isMatched).negate()).map(ObjectOberservation::getObject).collect(Collectors.toList());
		
		ranking.register(previousObjects, newObjects);
		
		
		for (ObjectOberservation<T> current : newObservations) {
			if(current.isMatched())
				continue;
			
			for (TrackedObject<T> previous : trackedObjects) {
				if(previous.getLastActiveRevision() == r)
					continue;
				
				V value = ranking.getValue(previous, current, r);
				if (value != null) {
					MatchPair<T> match = new MatchPair<>(previous, current);
					list.add(match);
					values.put(match, value);
				}
			}
		}
	}

	private <V> void addCandidatesWithinLimit(Ranking<T, V> ranking, RevisionData r,
			Map<MatchPair<T>, V> values, List<MatchPair<T>> active, PeekingIterator<MatchPair<T>> iter,
			MatchPair<T> best) {
		Predicate<V> limit = ranking.getRelaxor(values.get(best));
		while (iter.hasNext() && limit.test(values.get(iter.peek()))) {
			MatchPair<T> next = iter.next();
			if (next.stillAvailabe(r))
				active.add(next);
		}
	}

	private List<MatchPair<T>> updateActiveList(RevisionData r, List<MatchPair<T>> active,
			PeekingIterator<MatchPair<T>> iter) {
		if (active.isEmpty()) {
			MatchPair<T> next = iter.next();
			// did we already match one of the involved elements?
			if (next.stillAvailabe(r))
				active.add(next);
		} else {
			active = active.stream().filter(s -> s.stillAvailabe(r)).collect(Collectors.toList());
		}
		return active;
	}
	@Override
	public String toString() {
		return "Matcher [rankings=" + rankings.stream().map(Ranking::toString).collect(Collectors.joining("|")) + "]";
	}
	
	
}
