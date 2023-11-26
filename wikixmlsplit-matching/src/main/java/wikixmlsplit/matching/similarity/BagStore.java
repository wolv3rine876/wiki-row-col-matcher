package wikixmlsplit.matching.similarity;


import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class BagStore<T> {
	private Map<T,WeakReference<T>> map = new WeakHashMap<>();
	
	public T get(T obj) {
		return map.computeIfAbsent(obj, a -> new WeakReference<>(obj)).get();
	}
}
