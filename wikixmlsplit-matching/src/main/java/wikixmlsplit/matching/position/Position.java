package wikixmlsplit.matching.position;

public interface Position {

	String getPositionString();
	
	int getDifference(Position other);
}
