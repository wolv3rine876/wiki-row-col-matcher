package wikixmlsplit.anchored.features;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import wikixmlsplit.evaluation.evaluators.AnchoredCellValue;
import wikixmlsplit.matching.similarity.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ColumnIntersectCounter<T> {
	
	private Function<T, List<String>> extract;

	public ColumnIntersectCounter(Function<T, List<String>> extract) {
		this.extract = extract;
	}

	LoadingCache<Pair<T>, Multiset<AnchoredCellValue>> cache = CacheBuilder.newBuilder().maximumSize(10000)
			.build(new CacheLoader<>() {

				@Override
				public Multiset<AnchoredCellValue> load(Pair<T> key) {
					return getCellMultiset(key.getObject1(), key.getObject2());
				}

			});

	public double[][] getIntersectCount(List<T> columns1, List<T> columns2, int memberColumn1, int memberColumn2) {
		double[][] result = new double[columns1.size()][columns2.size()];

		List<Multiset<AnchoredCellValue>> multisets1 = getAnchoredCells(columns1, memberColumn1);
		List<Multiset<AnchoredCellValue>> multisets2 = getAnchoredCells(columns2, memberColumn2);
		fillResult(columns1, columns2, memberColumn1, memberColumn2, result, multisets1, multisets2);
		return result;
	}
	
	public Multiset<AnchoredCellValue> getCellMultiset(T r1, T row) {
		Multiset<AnchoredCellValue> thisMultiset = HashMultiset.create();
		int i = 0;
		List<String> subjectColumn = extract.apply(row);
		for (String s : extract.apply(r1)) {
			thisMultiset.add(new AnchoredCellValue(subjectColumn.size() > i ? subjectColumn.get(i) : null,s));
			i++;
		}
		return thisMultiset;
	}

	private List<Multiset<AnchoredCellValue>> getAnchoredCells(List<T> columns1, int memberColumn1) {
		T member1 = columns1.get(memberColumn1);
		List<Multiset<AnchoredCellValue>> multisets1 = new ArrayList<>(columns1.size());
		for (T r : columns1) {
			multisets1.add(cache.getUnchecked(new Pair<>(r, member1)));
		}
		return multisets1;
	}

	private void fillResult(List<T> columns1, List<T> columns2, int memberColumn1, int memberColumn2,
			double[][] result, List<Multiset<AnchoredCellValue>> multisets1,
			List<Multiset<AnchoredCellValue>> multisets2) {
		for (int i = 0; i < columns1.size(); ++i) {
			for (int j = 0; j < columns2.size(); ++j) {

				if (i == memberColumn1 ^ j == memberColumn2) {
					result[i][j] = 9999999.0;
				} else {
					result[i][j] = -Multisets.intersection(multisets1.get(i), multisets2.get(j)).size();
				}

			}
		}
	}
}
