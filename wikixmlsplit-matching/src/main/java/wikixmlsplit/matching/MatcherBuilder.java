package wikixmlsplit.matching;

import wikixmlsplit.matching.ranking.ActiveTimeRanking;
import wikixmlsplit.matching.ranking.PositionRanking;
import wikixmlsplit.matching.ranking.Ranking;
import wikixmlsplit.matching.ranking.SimilarityRanking;
import wikixmlsplit.matching.similarity.SimilarityMeasure;

import java.util.ArrayList;
import java.util.List;

public class MatcherBuilder<T> {

	private List<Ranking<T, ?>> rankings = new ArrayList<>();

	public MatcherBuilder<T> addRanking(Ranking<T, ?> rank) {
		rankings.add(rank);
		return this;
	}

	// convenience function for similarity rankings
	public MatcherBuilder<T> addSimilarityRanking(SimilarityMeasure<? super T> sim, double limit, double relaxLimit) {
		rankings.add(new SimilarityRanking<>(sim, limit, relaxLimit));
		return this;
	}

	public Matcher<T> createMatcher(boolean greedy) {
		return createMatcher(greedy, false);
	}
	
	public Matcher<T> createMatcher(boolean greedy, boolean ignorePosition) {
		return greedy ? new MatcherGreedy<>(rankings) : new MatcherHungarian<>(rankings, ignorePosition);
	}


	public static <V> Matcher<V> createDefaultSimMatcher(SimilarityMeasure<? super V> sim, double limit, double relaxLimit) {
		return createDefaultSimMatcher(sim, limit, relaxLimit, true);
	}
	
	public static <V> Matcher<V> createDefaultSimMatcher(SimilarityMeasure<? super V> sim, double limit, double relaxLimit, boolean greedy) {
		return new MatcherBuilder<V>().addSimilarityRanking(sim, limit, relaxLimit).addRanking(new ActiveTimeRanking<>())
				.addRanking(new PositionRanking<>()).createMatcher(greedy);
	}
}
