package wikixmlsplit.evaluation.data;

import java.util.HashMap;
import java.util.Map;

public class PairEvaluationResult {

	private final int truePositive;
	private final int trueNewCluster;
	private final int falseNewCluster;
	private final int wrongMatch;
	private final int missingNewCluster;

	public PairEvaluationResult(int truePositive, int trueNewCluster, int falseNewCluster, int wrongMatch,
			int missingNewCluster) {
		super();
		this.truePositive = truePositive;
		this.trueNewCluster = trueNewCluster;
		this.falseNewCluster = falseNewCluster;
		this.wrongMatch = wrongMatch;
		this.missingNewCluster = missingNewCluster;
	}

	public int getTruePositive() {
		return truePositive;
	}

	public int getTrueNewCluster() {
		return trueNewCluster;
	}

	public int getFalseNewCluster() {
		return falseNewCluster;
	}

	public int getWrongMatch() {
		return wrongMatch;
	}

	public int getMissingNewCluster() {
		return missingNewCluster;
	}

	public double getRandIndex() {
		return (0.0d + truePositive + trueNewCluster)
				/ (truePositive + trueNewCluster + falseNewCluster + missingNewCluster + wrongMatch);
	}

	@Override
	public String toString() {
		return "PairEvaluationResult [truePositive=" + truePositive + ", trueNewCluster=" + trueNewCluster
				+ ", falseNewCluster=" + falseNewCluster + ", wrongMatch=" + wrongMatch + ", missingNewCluster="
				+ missingNewCluster + ", getRandIndex()=" + getRandIndex() + "]";
	}

	public String getResultList() {
		return truePositive + "," + trueNewCluster + "," + falseNewCluster + "," + wrongMatch + "," + missingNewCluster
				+ "," + getRandIndex();
	}

	public Map<? extends String, ?> getResultMap() {
		Map<String, Object> result = new HashMap<>();
		result.put("truePositive", truePositive);
		result.put("trueNewCluster", trueNewCluster);
		result.put("falseNewCluster", falseNewCluster);
		result.put("wrongMatch", wrongMatch);
		result.put("missingNewCluster", missingNewCluster);
		return result;
	}

}
