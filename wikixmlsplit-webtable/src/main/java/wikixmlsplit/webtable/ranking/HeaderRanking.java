package wikixmlsplit.webtable.ranking;

import info.debatty.java.stringsimilarity.LongestCommonSubsequence;
import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.ranking.Ranking;
import wikixmlsplit.webtable.Webtable;

import java.util.Comparator;
import java.util.function.Predicate;

public class HeaderRanking implements Ranking<Webtable, Integer> {

	private LongestCommonSubsequence lcs = new LongestCommonSubsequence();
	
	@Override
	public Integer getValue(TrackedObject<Webtable> old, ObjectOberservation<Webtable> current, RevisionData r) {
		return lcs.length(old.getObject().getHeadings(), current.getObject().getHeadings());
	}

	@Override
	public Comparator<Integer> getComparator() {
		return Comparator.<Integer>comparingInt(j -> j).reversed();
	}

	@Override
	public Predicate<Integer> getRelaxor(Integer best) {
		return Predicate.isEqual(best);
	}

}
