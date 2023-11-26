package wikixmlsplit.socrata.input;

import java.util.Arrays;
import java.util.List;

public class DatasetChangeSummary {

	private String id;
	private List<String> versionsWithChanges;
	private List<String> deletions;

	public String getId() {
		return id;
	}

	public List<String> getVersionsWithChanges() {
		return versionsWithChanges;
	}

	public List<String> getDeletions() {
		return deletions;
	}

	public List<List<String>> getChangesAndDeletes() {
		return Arrays.asList(versionsWithChanges, deletions);
	}

	@Override
	public String toString() {
		return "DatasetChangeSummary [id=" + id + "]";
	}
}
