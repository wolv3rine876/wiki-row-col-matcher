package wikixmlsplit.socrata;

import wikixmlsplit.matching.data.RevisionData;

import java.math.BigInteger;
import java.time.Instant;

public class SocrataRevision implements RevisionData {

	private BigInteger id;
	private Instant date;

	public SocrataRevision(BigInteger id, Instant date) {
		super();
		this.id = id;
		this.date = date;
	}

	@Override
	public Instant getInstant() {
		return date;
	}

	@Override
	public BigInteger getId() {
		return id;
	}

	@Override
	public String toString() {
		return "SocrataRevision [id=" + id + ", date=" + date + "]";
	}

}
