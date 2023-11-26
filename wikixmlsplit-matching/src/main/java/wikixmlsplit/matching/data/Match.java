package wikixmlsplit.matching.data;

import wikixmlsplit.matching.position.Position;

import java.math.BigInteger;

/*
 * Represents a matching (clusterId, revisionId, position)
 */
public class Match<T extends Position> {

	private final String clusterIdentifier;
	private final BigInteger revisionId;
	private final T position;
	private final boolean objectChanged;

	public Match(String clusterIdentifier, BigInteger revisionId, T position, boolean objectChanged) {
		super();
		this.clusterIdentifier = clusterIdentifier;
		this.revisionId = revisionId;
		this.position = position;
		this.objectChanged = objectChanged;
	}

	public String getClusterIdentifier() {
		return clusterIdentifier;
	}

	public BigInteger getRevisionId() {
		return revisionId;
	}

	public T getPosition() {
		return position;
	}
	
	public String getObjectId() {
		return revisionId + ":" + position.getPositionString();
	}

	public boolean isObjectChanged() {
		return objectChanged;
	}
	@Override
	public String toString() {
		return "Match [clusterIdentifier=" + clusterIdentifier + ", revisionId=" + revisionId + ", position=" + position
				+ ", objectChanged=" + objectChanged + "]";
	}

}