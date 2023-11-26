package wikixmlsplit.matching.data;

import wikixmlsplit.matching.position.Position;

public class ObjectOberservation<T> {
	private boolean matched;
	private final T object;
	private final Position position;
	
	public ObjectOberservation(T object, Position position) {
		this.object = object;
		this.position = position;
		this.matched = false;
	}
	
	public T getObject() {
		return object;
	}
	
	public Position getPosition() {
		return position;
	}
	
	public boolean isMatched() {
		return matched;
	}
	
	public void markMatched() {
		matched = true;
	}
}
