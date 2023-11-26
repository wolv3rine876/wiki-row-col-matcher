package wikixmlsplit.lists.builder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.sweble.wikitext.parser.nodes.*;
import wikixmlsplit.io.BuilderBase;
import wikixmlsplit.lists.WikiList;

public class WikiListBuilder extends BuilderBase<WikiList> {

	public WikiListBuilder() {
		nodeCache = CacheBuilder.newBuilder().recordStats().maximumSize(1000).build(new CacheLoader<>() {

			@Override
			public WikiList load(Input i) {
				WikiList infobox = (WikiList) (new WikiListVisitor("").go(i.getNode()));
				infobox.setHeadings(i.getHeadings());
				infobox.setNode(i.getNode());
				return infobox;
			}

		});
	}

	protected String getNodeType() {
		return "LISTS";
	}

	protected boolean isTargetNode(WtNode n) {
		return n instanceof WtDefinitionList || n instanceof WtOrderedList || n instanceof WtUnorderedList
				|| n instanceof WtXmlElement;
	}

}
