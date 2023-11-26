package wikixmlsplit.infobox.builder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtTemplate;
import wikixmlsplit.infobox.Infobox;
import wikixmlsplit.io.BuilderBase;

public class InfoboxBuilder extends BuilderBase<Infobox> {

	public InfoboxBuilder() {
		nodeCache = CacheBuilder.newBuilder().recordStats().maximumSize(1000).build(new CacheLoader<>() {

			@Override
			public Infobox load(Input i) {
				Infobox infobox = (Infobox) (new InfoboxVisitor("").go(i.getNode()));
				infobox.setHeadings(i.getHeadings());
				infobox.setNode(i.getNode());
				return infobox;
			}

		});
	}


	protected String getNodeType() {
		return "INFOBOX";
	}

	protected boolean isTargetNode(WtNode n) {
		return n instanceof WtTemplate;
	}

}
