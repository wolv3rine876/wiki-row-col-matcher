package wikixmlsplit.output.renderer;

import org.sweble.wikitext.parser.nodes.WtNode;

public class RenderResult {
	private String hashCode;
	private String position;
	private WtNode node;
	private String heading;
	private String caption;

	public RenderResult(String hashCode, String position, WtNode node, String heading, String caption) {
		super();
		this.hashCode = hashCode;
		this.position = position;
		this.node = node;
		this.heading = heading;
		this.caption = caption;
	}

	public String getHashCode() {
		return hashCode;
	}

	public String getPositionString() {
		return position;
	}

	public WtNode getNode() {
		return node;
	}

	public String getHeadings() {
		return heading;
	}

	public String getCaption() {
		return caption;
	}
}
