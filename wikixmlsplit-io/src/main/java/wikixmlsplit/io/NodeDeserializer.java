package wikixmlsplit.io;

import com.google.common.base.Objects;
import wikixmlsplit.datastructures.MyRevisionType;
import wikixmlsplit.datastructures.MyRevisionTypeExtended;
import wikixmlsplit.datastructures.UniversalRevisionType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class NodeDeserializer {
	private final long timeResolution;

	public NodeDeserializer() {
		this(0);
	}

	public NodeDeserializer(long timeResolution) {
		this.timeResolution = timeResolution;
	}

	public long getTimeResolution() {
		return timeResolution;
	}

	public void deserialize(List<MyRevisionType> revisions,
			BiConsumer<MyRevisionType, Map<String, List<String>>> consumer) {
		Map<String, List<String>> last = null;
		long lastPeriod = 0;
		for (int i = 0; i < revisions.size(); ++i) {
			if (timeResolution > 0 && i + 1 < revisions.size()) {
				long newPeriod = (revisions.get(i + 1).getInstant().toEpochMilli() - 1L) / timeResolution;
				if (newPeriod == lastPeriod) {
					continue;
				}
				lastPeriod = newPeriod;
			}

			MyRevisionType r = revisions.get(i);

			Map<String, List<String>> parsed = new HashMap<>();
			parsed.put("TABLE", Collections.emptyList());
			parsed.put("INFOBOX", Collections.emptyList());
			parsed.put("LISTOFPAGES", Collections.emptyList());
			parsed.put("REDIRECTS", Collections.emptyList());
			parsed.put("CATEGORIES", Collections.emptyList());
			parsed.put("TEMPLATES", Collections.emptyList());
			parsed.put("LISTS", Collections.emptyList());
			if (r instanceof UniversalRevisionType) {
				parsed.putAll(((UniversalRevisionType) r).getParsedMap());
			} else if (r instanceof MyRevisionTypeExtended) {
				if (((MyRevisionTypeExtended) r).getCategories() != null)
					parsed.put("CATEGORIES", ((MyRevisionTypeExtended) r).getCategories());
				if (((MyRevisionTypeExtended) r).getTemplateNames() != null)
					parsed.put("TEMPLATES", ((MyRevisionTypeExtended) r).getTemplateNames());
				if (r.getParsed() != null)
					parsed.put("TABLE", r.getParsed());
			} else {
				if (r.getParsed() != null)
					parsed.put("TABLE", r.getParsed());
			}

			if (!Objects.equal(last, parsed))
				consumer.accept(r, parsed);

			last = parsed;
		}

	}

}
