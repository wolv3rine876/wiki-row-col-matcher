package wikixmlsplit.webtable.ranking;

import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.ranking.Ranking;
import wikixmlsplit.webtable.Webtable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class SchemaRanking implements Ranking<Webtable, Boolean> {

	@Override
	public Boolean getValue(TrackedObject<Webtable> old, ObjectOberservation<Webtable> current, RevisionData r) {
		if(old.getObject().getRows().isEmpty() || current.getObject().getRows().isEmpty())
			return false;
		
		List<String> header1 = old.getObject().getRows().get(0);
		List<String> header2 = current.getObject().getRows().get(0);
		return Objects.equals(header1, header2);
	}

	@Override
	public Comparator<Boolean> getComparator() {
		return Comparator.<Boolean, Boolean>comparing(i -> i).reversed();
	}

	@Override
	public Predicate<Boolean> getRelaxor(Boolean best) {
		return Predicate.isEqual(best);
	}

}
