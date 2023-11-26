package wikixmlsplit.matching.ranking;

import wikixmlsplit.matching.data.MatchPair;
import wikixmlsplit.matching.data.RevisionData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IntermediateRankingResult<T> {

	private final List<MatchPair<T>> matchList;
	private final RevisionData revision;

	public IntermediateRankingResult(RevisionData r, List<MatchPair<T>> list) {
		this.revision = r;
		this.matchList = Collections.unmodifiableList(list);
	}

	public RevisionData getRevision() {
		return revision;
	}

	public List<MatchPair<T>> getList() {
		return matchList;
	}

	public <V> void refine(Ranking<T, V> ranking, Consumer<IntermediateRankingResult<T>> consumer) {
		if (matchList.size() < 2) {
			consumer.accept(this);
			return;
		}

		Map<MatchPair<T>, V> values = new HashMap<>(matchList.size());

		V best = null;
		for (MatchPair<T> match : matchList) {
			V value = ranking.getValue(match.getPrevious(), match.getCurrent(), revision);
			if (value != null) {
				values.put(match, value);
				if (best == null || ranking.getComparator().compare(value, best) < 0)
					best = value;
			}
		}

		Predicate<V> limit = ranking.getRelaxor(best);

		List<MatchPair<T>> refinedCandidates = matchList.stream().filter(i -> limit.test(values.get(i)))
				.collect(Collectors.toList());
		consumer.accept(new IntermediateRankingResult<>(revision, refinedCandidates));
	}

	public <V> void getBest(Ranking<T, V> ranking, Consumer<MatchPair<T>> consumer) {
		if (matchList.size() < 2) {
			consumer.accept(matchList.get(0));
			return;
		}

		V best = null;
		MatchPair<T> bestM = null;
		for (MatchPair<T> match : matchList) {
			V value = ranking.getValue(match.getPrevious(), match.getCurrent(), revision);
			if (value != null && (best == null || ranking.getComparator().compare(value, best) < 0)) {
				best = value;
				bestM = match;
			}
		}
		consumer.accept(bestM);
	}
}
