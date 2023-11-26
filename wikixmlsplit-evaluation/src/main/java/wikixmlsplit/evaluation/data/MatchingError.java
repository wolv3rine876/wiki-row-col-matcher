package wikixmlsplit.evaluation.data;

public class MatchingError {
	
	
	

	public enum ErrorType {
		TP, FN, FP, FN_FP
	}
	
	private final ErrorType type;
	private final ErrorType errorTypeBaseline;
	private final boolean trivial;
	

	private final String filename2;
	
	private final String filename1Output;
	private final String filename1Gold;
	private final String clustername1Gold;
	private final String clustername2Gold;
	
	
	public MatchingError(ErrorType type, ErrorType errorTypeBaseline, boolean trivial, String filename1,
			String filename2, String filename1Gold, String clustername1Gold, String clustername2Gold) {
		super();
		this.type = type;
		this.errorTypeBaseline = errorTypeBaseline;
		this.trivial = trivial;
		this.filename1Output = filename1;
		this.filename2 = filename2;
		this.filename1Gold = filename1Gold;
		this.clustername1Gold = clustername1Gold;
		this.clustername2Gold = clustername2Gold;
	}

	public ErrorType getBaselineErrorType() {
		return errorTypeBaseline;
	}

	public ErrorType getType() {
		return type;
	}

	public boolean isTrivial() {
		return trivial;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		result.append(type).append(": ").append(filename2).append("(").append(clustername2Gold).append(") was");
		if(clustername1Gold != null) {
			result.append(" matched to ").append(filename1Output).append("(").append(clustername1Gold).append(")");
		} else {
			result.append(" not matched");
		}
		
		if(filename1Gold.isEmpty()) {
			result.append(" but should not have been matched");
		} else {
			result.append(" but should have been matched to ").append(filename1Gold);
		}
		
		return result.toString(); 
	}

	public ErrorType getErrorTypeBaseline() {
		return errorTypeBaseline;
	}

	public String getFilename2() {
		return filename2;
	}

	public String getFilename1Output() {
		return filename1Output;
	}

	public String getFilename1Gold() {
		return filename1Gold;
	}

	public String getClustername1Gold() {
		return clustername1Gold;
	}

	public String getClustername2Gold() {
		return clustername2Gold;
	}

}
