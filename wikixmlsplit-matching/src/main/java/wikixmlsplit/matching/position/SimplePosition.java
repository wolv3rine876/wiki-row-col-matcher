package wikixmlsplit.matching.position;

public abstract class SimplePosition implements Position {

	private final int position;
	
	public SimplePosition(int position) {
		this.position = position;
	}
	
	public int getIndex() {
		return position;
	}
	
	@Override
	public String getPositionString() {
		return Integer.toString(position);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + position;
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
		SimplePosition other = (SimplePosition) obj;
		if (position != other.position)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "SimplePosition [position=" + position + "]";
	}

	@Override
	public int getDifference(Position other) {
		if(!(other instanceof SimplePosition))
			throw new IllegalArgumentException("can only get difference to same type");
		
		SimplePosition otherSP = (SimplePosition) other;
		return Math.abs(position - otherSP.position);
	}

}
