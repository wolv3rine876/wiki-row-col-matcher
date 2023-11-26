package wikixmlsplit.util;

import de.fau.cs.osr.ptk.common.AstVisitor;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtText;

public class TextExtractVisitor extends AstVisitor<WtNode> {

	private StringBuilder b;
	// =========================================================================

	@Override
	protected WtNode before(WtNode node) {
		// This method is called by go() before visitation starts
		b = new StringBuilder();
		return super.before(node);
	}

	@Override
	protected Object after(WtNode node, Object result) {

		// This method is called by go() after visitation has finished
		// The return value will be passed to go() which passes it to the caller
		return b.toString().trim();
	}

	// =========================================================================

	public void visit(WtNode n) {
		iterate(n);
	}

	public void visit(WtText node) {
		b.append(node.getContent());
	}

}
