package wikixmlsplit.evaluation.evaluators;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SubjectColumnLoader {
	
	public static Map<String, Integer> loadSubjectColumns(Path inputPath) throws IOException {
		Gson g = new Gson();
		Map<String, Integer> result = new HashMap<>();
		Comparator<SubjectColumnInfo> comp = Comparator.comparing(i -> -i.getScore());
		try (Stream<String> lines = Files.lines(inputPath)) {
			lines.forEach(i -> {
				SubjectColumn map = g.fromJson(i, SubjectColumn.class);
				if(map.getSubjectColumns() != null && !map.getSubjectColumns().isEmpty()) {
					map.getSubjectColumns().sort(comp);
					result.put(map.getFilename().split("-",2)[1], map.getSubjectColumns().get(0).getColumnIndex());
				} 
			});
		}
		
		return result;
	}
	
	private static class SubjectColumn {
		private String filename;
		private List<SubjectColumnInfo> subjectColumns;
		public String getFilename() {
			return filename;
		}
		public List<SubjectColumnInfo> getSubjectColumns() {
			return subjectColumns;
		}
	}
	
	private static class SubjectColumnInfo {
		private double score;
		private boolean acroynm;
		private int columnIndex;
		public double getScore() {
			return score;
		}
		public boolean isAcroynm() {
			return acroynm;
		}
		public int getColumnIndex() {
			return columnIndex;
		}
	}
}
