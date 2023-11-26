package wikixmlsplit.wikitable;

class ActiveSpan {
	private int remainingRows;
	private int columns;
	private Cell cell;
	
	public ActiveSpan(int remainingRows, int columns, Cell cell) {
		this.remainingRows = remainingRows;
		this.columns = columns;
		this.cell = cell;
	}

	public int getColumns() {
		return columns;
	}

	public Cell getCell() {
		return cell;
	}
	
	public boolean takeRow() {
		--remainingRows;
		return remainingRows <= 0;
	}
}
