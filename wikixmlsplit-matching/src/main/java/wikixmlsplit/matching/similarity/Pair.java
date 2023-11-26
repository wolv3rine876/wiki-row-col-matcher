package wikixmlsplit.matching.similarity;

public class Pair<T> {
	private T object1;
	private T object2;
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object1 == null) ? 0 : System.identityHashCode(object1));
		result = prime * result + ((object2 == null) ? 0 : System.identityHashCode(object2));
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
		Pair<?> other = (Pair<?>) obj;
		return object1 == other.object1 && object2 == other.object2;
	}
	public Pair(T words1, T words2) {
		super();
		this.object1 = words1;
		this.object2 = words2;
	}
	public T getObject1() {
		return object1;
	}
	public T getObject2() {
		return object2;
	}
	
	
}
