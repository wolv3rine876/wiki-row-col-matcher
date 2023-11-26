package wikixmlsplit.wikitable.builder;

import de.fau.cs.osr.utils.StringUtils;
import wikixmlsplit.wikitable.Cell;
import wikixmlsplit.wikitable.WikiTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

class TableFactory {

	private static final Pattern ws = Pattern.compile("\\s+");

	private StringBuilder sb;
	private List<Cell> currentRow;
	private List<List<Cell>> rows;
	private String caption;
	private Map<String, Object> currentProps  = null;

	private boolean pastBod;
	private int needNewlines;
	private boolean needSpace;

	public TableFactory() {
		sb = new StringBuilder();
		pastBod = false;
		needNewlines = 0;
		needSpace = false;
		rows = new ArrayList<>();
		currentRow = new ArrayList<>();
	}

	public void write(String s) {
		if (s.isEmpty())
			return;

		if (Character.isSpaceChar(s.charAt(0)))
			wantSpace();

		String[] words = ws.split(s);
		for (int i = 0; i < words.length;) {
			writeWord(words[i]);
			if (++i < words.length)
				wantSpace();
		}

		if (Character.isSpaceChar(s.charAt(s.length() - 1)))
			wantSpace();
	}

	public void write(char[] cs) {
		write(String.valueOf(cs));
	}

	public void write(char ch) {
		write(String.valueOf(ch));
	}

	public void write(int num) {
		write(String.valueOf(num));
	}

	public void newRow() {
		currentRow = new ArrayList<>();
	}

	public void finishRow() {
		rows.add(currentRow);
	}

	public void newCell() {
		sb.setLength(0);
		currentProps = null;
		pastBod = false;
		needNewlines = 0;
		needSpace = false;
	}

	public void finishCell() {
		currentRow.add(new Cell(sb.toString(), currentProps));
	}

	public void newHeader() {
		newCell();
		addProperty("header", true);
	}

	public void finishHeader() {
		currentRow.add(new Cell(sb.toString(), currentProps));
	}

	public void newCaption() {
		sb.setLength(0);
		pastBod = false;
		needNewlines = 0;
		needSpace = false;
	}

	public void finishCaption() {
		caption = sb.toString();
	}

	public WikiTable getOutput(boolean useSpans) {
		return new WikiTable(caption, rows, useSpans);
	}

	public void newline(int num) {
		if (pastBod) {
			if (num > needNewlines)
				needNewlines = num;
		}
	}

	private void wantSpace() {
		if (pastBod)
			needSpace = true;
	}

	private void writeWord(String s) {
		if (s.length() == 0)
			return;

		if (needSpace && needNewlines <= 0)
			sb.append(' ');

		if (needNewlines > 0) {
			sb.append(StringUtils.strrep('\n', needNewlines));
			needNewlines = 0;
		}

		needSpace = false;
		pastBod = true;
		sb.append(s);
	}

	public void setRowSpan(int value) {
		addProperty("rowspan", value);
	}

	public void setColSpan(int value) {
		addProperty("colspan", value);
	}
	
	private void addProperty(String key, Object value) {
		if(this.currentProps == null)
			this.currentProps = new HashMap<>();
		this.currentProps.put(key, value);
	}
	
	public int getRowCount() {
		return rows.size();
	}

}
