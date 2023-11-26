package wikixmlsplit.evaluation.evaluators;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemberColumnDetector<T> {

	private Pattern p = Pattern.compile("\\d");
	private Function<T, Iterable<String>> extract;

	public MemberColumnDetector(Function<T, Iterable<String>> extract) {
		this.extract = extract;
	}

	public int find(List<T> columns) {
		int i = 0;
		for (T column : columns) {
			int numberCount = 0;
			int size = 0;
			for (String value : extract.apply(column)) {
				Matcher m = p.matcher(value);
				int count = 0;
				while (m.find()) {
					count++;
				}
				if (((double) count) / value.length() >= 0.2d) {
					++numberCount;
				}

				++size;
			}
			if (((double) numberCount) / size <= 0.5 && size >= 2) {
                return i;
			}
			++i;
		}
		// use first column otherwise
		return 0;
	}

}
