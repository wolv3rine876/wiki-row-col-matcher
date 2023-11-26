package wikixmlsplit.datastructures;

import java.util.ArrayList;
import java.util.List;

public class MyRevisionTypeExtended extends MyRevisionType {


	protected ArrayList<String> categories;
	protected ArrayList<String> templateNames;
	
	public MyRevisionTypeExtended() {
		super();
	}

	public MyRevisionTypeExtended(MyRevisionType o) {
		super(o);
	}

	public void addCatgories(List<String> categories) {
		this.categories = new ArrayList<>(categories);
	}
	
	public List<String> getCategories() {
		return categories;
	}
	
	public void addTemplateNames(List<String> templateNames) {
		this.templateNames = new ArrayList<>(templateNames);
	}
	
	public List<String> getTemplateNames() {
		return templateNames;
	}
}
