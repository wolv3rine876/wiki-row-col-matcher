package wikixmlsplit.matching.data;

public class MatchPair<T> {
	private final TrackedObject<T> previous;
	private final ObjectOberservation<T> current;

	public MatchPair(TrackedObject<T> previous, ObjectOberservation<T> current) {
		super();
		this.previous = previous;
		this.current = current;
	}

	public ObjectOberservation<T> getCurrent() {
		return current;
	}

	public TrackedObject<T> getPrevious() {
		return previous;
	}

	public boolean stillAvailabe(RevisionData r) {
		return !current.isMatched() && previous.getLastActiveRevision() != r;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + current.hashCode();
		result = prime * result + previous.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MatchPair<?> other = (MatchPair<?>) obj;
		if (current != other.current)
			return false;
		if (previous != other.previous)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MatchPair [previousIndex=" + previous.getCurrentPosition() + ", newIndex=" + current.getPosition() + "]";
	}
}
