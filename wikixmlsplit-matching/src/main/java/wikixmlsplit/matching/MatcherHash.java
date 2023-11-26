package wikixmlsplit.matching;

import wikixmlsplit.matching.data.MatchPair;
import wikixmlsplit.matching.data.ObjectOberservation;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MatcherHash<T> implements Matcher<T> {

	@Override
	public void match(RevisionData r, List<TrackedObject<T>> trackedObjects,
			List<ObjectOberservation<T>> newObservations, Consumer<MatchPair<T>> consumer) {
		Map<T, TrackedObject<T>> objects = new HashMap<>();
		for(TrackedObject<T> t : trackedObjects) {
			TrackedObject<T> previous = objects.put(t.getObject(), t);
			if(previous != null) {
				if(previous.getActiveTime(r) > t.getActiveTime(r)) {
					objects.put(t.getObject(), previous);
				}
			}
		}
		
		for(ObjectOberservation<T> o : newObservations) {
			TrackedObject<T> tracked = objects.remove(o.getObject());
			if(tracked != null) {
				consumer.accept(new MatchPair<>(tracked, o));
			}
		}
	}

}
