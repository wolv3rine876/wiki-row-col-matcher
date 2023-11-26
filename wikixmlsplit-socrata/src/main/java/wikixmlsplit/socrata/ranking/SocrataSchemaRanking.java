package wikixmlsplit.socrata.ranking;

import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.ranking.Ranking;
import wikixmlsplit.socrata.SocrataTable;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

public class SocrataSchemaRanking implements Ranking<SocrataTable, Boolean> {

	@Override
	public Boolean getValue(TrackedObject<SocrataTable> old, ObjectOberservation<SocrataTable> current, RevisionData r) {
		if(old.getObject().getRows().isEmpty() || current.getObject().getRows().isEmpty())
			return false;
		
		return Objects.equals(old.getObject().getHeader(), current.getObject().getHeader());
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
