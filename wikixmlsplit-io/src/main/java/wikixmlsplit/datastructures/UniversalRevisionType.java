package wikixmlsplit.datastructures;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UniversalRevisionType extends MyRevisionType {

	private Map<String, List<String>> parsed;
	
	public UniversalRevisionType() {
		super();
	}

	public UniversalRevisionType(MyRevisionType o) {
		super(o);
		this.parsed = Collections.emptyMap();
	}
	
	public UniversalRevisionType(MyRevisionType o, Map<String, List<String>> parsed) {
		super(o);
		this.parsed = parsed;
	}


	public List<String> getParsed() {
		throw new IllegalAccessError();
	}


	public void addParsed(String parsed) {
		throw new IllegalAccessError();
	}
	
	
	
	public List<String> getParsed(String type) {
		return parsed.get(type);
	}

	public Map<String, List<String>> getParsedMap() {
		return parsed;
	}

	@Override
	public String toString() {
		return "UniversalRevisionType [parsed=" + parsed + ", id=" + id + ", parentid=" + parentid + ", timestamp="
				+ timestamp + ", contributor=" + contributor + ", minor=" + minor + ", comment=" + comment + ", model="
				+ model + ", format=" + format + ", text=" + text + ", sha1=" + sha1 + ", instant=" + instant + "]";
	}

}
