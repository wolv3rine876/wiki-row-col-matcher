
package wikixmlsplit.parser.visitors;

import de.fau.cs.osr.ptk.common.AstVisitor;
import org.sweble.wikitext.parser.nodes.WtEmptyImmutableNode;
import org.sweble.wikitext.parser.nodes.WtInnerImmutableNode1;
import org.sweble.wikitext.parser.nodes.WtNode;

/**
 * Removes some unneeded informations from the nodes.
 */
public final class MyStripVisitor extends AstVisitor<WtNode> {
	private boolean stripAllAttributes;

	private boolean stripRtdAttributes;

	private boolean stripLocations;

	// =========================================================================

	public MyStripVisitor(boolean stripAllAttributes, boolean stripRtdAttributes, boolean stripLocations) {
		this.stripAllAttributes = stripAllAttributes;
		this.stripRtdAttributes = stripRtdAttributes;
		this.stripLocations = stripLocations;
	}

	// =========================================================================

	public void visit(WtNode n) {
		if (stripAllAttributes) {
			n.clearAttributes();
		} else if (stripRtdAttributes) {
			n.clearRtd();
			n.removeAttribute("RTD");
		}

		if (stripLocations)
			n.setNativeLocation(null);

		iterate(n);
	}

	public void visit(WtInnerImmutableNode1 n) {
		iterate(n);
	}

	public void visit(WtEmptyImmutableNode n) {
		iterate(n);
	}
}
