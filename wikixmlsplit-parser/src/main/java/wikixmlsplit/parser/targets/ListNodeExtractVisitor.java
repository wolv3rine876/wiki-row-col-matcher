package wikixmlsplit.parser.targets;

import com.google.gson.Gson;
import de.fau.cs.osr.ptk.common.AstVisitor;
import org.sweble.wikitext.parser.nodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Only keeps headings and lists.
 */
public class ListNodeExtractVisitor extends AstVisitor<WtNode> {

	private int listDepth;
	private List<WtNode> nodes;

	private List<WtHeading> headings;
	
	private final Gson serializer;

	public ListNodeExtractVisitor(Gson serializer) {
		this.serializer = serializer;
	}
	
	// =========================================================================

	@Override
	protected WtNode before(WtNode node) {
		// This method is called by go() before visitation starts
		nodes = new ArrayList<>();
		headings = new ArrayList<>();
		listDepth = 0;
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

	public void visit(WtDefinitionList item) {
		newList(item);
	}

	public void visit(WtUnorderedList item) {
		newList(item);
	}

	public void visit(WtOrderedList item) {
		newList(item);
	}
	
	public void visit(WtXmlElement e) {
		switch (e.getName().toLowerCase()) {
		case "ul":
		case "ol":
			newList(e);
			break;
		default:
			iterate(e);
		}

	}

	private void newList(WtNode node) {
		++listDepth;
		if (listDepth == 1) {
			nodes.addAll(headings);
			nodes.add(node);
		}
		// iterate(node);
		--listDepth;
	}

	// =========================================================================

	public void visit(WtSection node) {
		if (listDepth == 0) {
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
