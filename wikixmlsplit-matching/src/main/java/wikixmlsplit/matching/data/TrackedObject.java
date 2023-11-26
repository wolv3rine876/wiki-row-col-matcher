package wikixmlsplit.matching.data;

import com.google.common.collect.EvictingQueue;
import wikixmlsplit.matching.position.Position;

import java.time.Duration;
import java.util.Collection;

public class TrackedObject<T> {
	private String identifier;
	
	private T prevObject;
	private T object;
	private EvictingQueue<T> objects;
	
	private RevisionData lastActiveRevision;
	private Position currentPosition;
	private boolean active;
	
	private long activeTime;
	private RevisionData activeTimeState;

	public TrackedObject(T object, RevisionData lastActiveRevision, Position currentPosition) {
		super();
		this.object = object;
		this.lastActiveRevision = lastActiveRevision;
		this.currentPosition = currentPosition;
		this.active = true;
		this.prevObject = null;
		this.activeTime = 0;
		this.activeTimeState = lastActiveRevision;
		this.objects = EvictingQueue.create(5);
		this.objects.add(object);
		this.identifier = lastActiveRevision.getId() + "-" + currentPosition.getPositionString();
	}

	public T getPrevObject() {
		return prevObject;
	}

	public T getObject() {
		return object;
	}


	public Position getCurrentPosition() {
		return currentPosition;
	}

	public RevisionData getLastActiveRevision() {
		return lastActiveRevision;
	}

	public boolean isActive() {
		return active;
	}

	public void update(T newObj, RevisionData r, Position position) {
		this.prevObject = active ? this.object : null;
		if (active) {
			activeTime += getPassedTime(r);
			activeTimeState = r;
		}
		if (!newObj.equals(object))
			this.objects.add(newObj);
		this.object = newObj;
		this.lastActiveRevision = r;

		this.currentPosition = position;
		this.active = true;

	}


	public Collection<T> getPastObjects() {
		return this.objects;
	}


	public long getActiveTime(RevisionData r) {
		if (r != activeTimeState) {
			activeTime += active ? getPassedTime(r) : 0;
			activeTimeState = r;
		}

		return activeTime;
	}

	public void setInactive(RevisionData r) {
		if (active) {
			activeTime += getPassedTime(r);
			activeTimeState = r;
		}
		this.active = false;
		this.prevObject = this.object;
	}

	public String getIdentifier() {
		return identifier;
	}
	

	private long getPassedTime(RevisionData r) {
		return Duration.between(activeTimeState.getInstant(), r.getInstant()).getSeconds();
	}
}
