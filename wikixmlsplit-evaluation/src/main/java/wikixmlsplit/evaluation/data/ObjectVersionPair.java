package wikixmlsplit.evaluation.data;

public class ObjectVersionPair {

	@Override
	public String toString() {
		return "TablePair [filename1=" + filename1 + ", cluster1=" + cluster1 + ", filename2=" + filename2
				+ ", cluster2=" + cluster2 + ", sameCluster=" + sameCluster + "]";
	}

	private final String filename1;
	private final String cluster1;

	private final String filename2;
	private final String cluster2;

	private boolean sameCluster;

	public ObjectVersionPair(String fileName1, String cluster1, String filename2, String cluster2) {
		this.filename1 = fileName1;
		this.cluster1 = cluster1;
		this.filename2 = filename2;
		this.cluster2 = cluster2;
		this.sameCluster = cluster1.equals(cluster2);
	}
	
	public String getFilename1() {
		return filename1;
	}

	public String getCluster1() {
		return cluster1;
	}

	public String getFilename2() {
		return filename2;
	}

	public String getCluster2() {
		return cluster2;
	}

	public boolean isSameCluster() {
		return sameCluster;
	}

	public String toRecord() {
		return String.join(",", filename1, cluster1, filename2, cluster2, "" + sameCluster);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filename1 == null) ? 0 : filename1.hashCode());
		result = prime * result + ((filename2 == null) ? 0 : filename2.hashCode());
		result = prime * result + (sameCluster ? 1231 : 1237);
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
		ObjectVersionPair other = (ObjectVersionPair) obj;
		if (filename1 == null) {
			if (other.filename1 != null)
				return false;
		} else if (!filename1.equals(other.filename1))
			return false;
		if (filename2 == null) {
			if (other.filename2 != null)
				return false;
		} else if (!filename2.equals(other.filename2))
			return false;
		if (sameCluster != other.sameCluster)
			return false;
		return true;
	}
}
