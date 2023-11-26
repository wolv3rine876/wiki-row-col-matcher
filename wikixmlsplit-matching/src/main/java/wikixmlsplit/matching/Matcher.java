package wikixmlsplit.matching;

import wikixmlsplit.matching.data.MatchPair;
import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;

import java.util.List;
import java.util.function.Consumer;

public interface Matcher<T> {
	void match(RevisionData r, List<TrackedObject<T>> trackedObjects,
               List<ObjectOberservation<T>> newObservations, Consumer<MatchPair<T>> consumer);
	
}
