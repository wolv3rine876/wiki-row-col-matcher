package wikixmlsplit.parser.targets;

import com.google.gson.Gson;
import de.fau.cs.osr.ptk.common.AstVisitor;
import org.sweble.wikitext.parser.nodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Only keeps headings and tables.
 */
public class TableNodeExtractVisitor extends AstVisitor<WtNode> {

	private int tableDepth;
	private List<WtNode> nodes;

	private List<WtHeading> headings;
	
	private final Gson serializer;

	public TableNodeExtractVisitor(Gson serializer) {
		this.serializer = serializer;
	}
	
	// =========================================================================

	@Override
	protected WtNode before(WtNode node) {
		// This method is called by go() before visitation starts
		nodes = new ArrayList<>();
		headings = new ArrayList<>();
		tableDepth = 0;
		return super.before(node);
	}

	@Override
	protected Object after(WtNode node, Object result) {

		// This method is called by go() after visitation has finished
		// The return value will be passed to go() which passes it to the caller
		return nodes.stream().map(serializer::toJson).collect(Collectors.toList());
	}

	// =========================================================================

	public void visit(WtNode n) {
		iterate(n);
	}

	/*
	 * ========================= TABLES
	 */

	public void visit(WtXmlElement e) {
		switch (e.getName().toLowerCase()) {
		case "table":
			newTable(e);
			break;
		default:
			iterate(e);
		}

	}

	public void visit(WtTable t) {
		newTable(t);
	}

	private void newTable(WtNode node) {
		++tableDepth;
		if (tableDepth == 1) {
			nodes.addAll(headings);
			nodes.add(node);
		}
		// iterate(node);
		--tableDepth;
	}

	// =========================================================================

	public void visit(WtSection node) {
		if (tableDepth == 0) {
			headings.add(node.getHeading());
			if (node.hasBody()) {
				iterate(node.getBody());
			}
			headings.remove(headings.size() - 1);
		} else {
			iterate(node);
		}
	}

}
