package wikixmlsplit.wikitable.builder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtTable;
import org.sweble.wikitext.parser.nodes.WtXmlElement;
import wikixmlsplit.io.BuilderBase;
import wikixmlsplit.wikitable.WikiTable;

public class TableBuilder extends BuilderBase<WikiTable> {

	public TableBuilder(boolean useSpans) {
		this(useSpans, -1);

	}

	public TableBuilder(boolean useSpans, int rowLimit) {
		this.nodeCache = CacheBuilder.newBuilder().recordStats().maximumSize(1000)
				.build(new CacheLoader<>() {

                    @Override
                    public WikiTable load(Input i) {
                        WikiTable table = (WikiTable) (new TableVisitor(useSpans, rowLimit).go(i.getNode()));
                        table.setHeadings(i.getHeadings());
                        table.setNode(i.getNode());
                        return table;
                    }

                });
	}

	protected String getNodeType() {
		return "TABLE";
	}

	protected boolean isTargetNode(WtNode n) {
		return n instanceof WtTable || n instanceof WtXmlElement;
	}

}
