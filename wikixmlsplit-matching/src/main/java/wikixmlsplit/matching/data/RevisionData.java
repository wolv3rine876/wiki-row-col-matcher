package wikixmlsplit.matching.data;

import java.math.BigInteger;
import java.time.Instant;

public interface RevisionData {

	Instant getInstant();

	BigInteger getId();

}
