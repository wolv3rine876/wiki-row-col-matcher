package wikixmlsplit.parser.targets;

import com.google.gson.Gson;
import de.fau.cs.osr.ptk.common.AstVisitor;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtTemplate;
import org.sweble.wikitext.parser.nodes.WtText;
import wikixmlsplit.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InfoboxNodeExtractVisitor extends AstVisitor<WtNode> {

	private int infoboxDepth;
	private List<WtNode> nodes;
	private StringBuilder builder = null;
	private final Gson serializer;

	public InfoboxNodeExtractVisitor(Gson serializer) {
		this.serializer = serializer;
	}

	// =========================================================================

	@Override
	protected WtNode before(WtNode node) {
		// This method is called by go() before visitation starts
		nodes = new ArrayList<>();
		infoboxDepth = 0;
		builder = null;
		return super.before(node);
	}

	@Override
	protected Object after(WtNode node, Object result) {
		// This method is called by go() after visitation has finished
		// The return value will be passed to go() which passes it to the caller
		return nodes.stream().map(serializer::toJson).collect(Collectors.toList());
	}

	public void visit(WtNode n) {
		iterate(n);
	}

	public void visit(WtTemplate t) {
		if (t.getName().isResolved()) {
			if (Util.containsIgnoreCase(t.getName().getAsString(), "infobox"))
				newInfobox(t);
		} else if (builder == null) { // if we are not already searching for a name
			builder = new StringBuilder();
			iterate(t.getName());

			if (Util.containsIgnoreCase(builder.toString(), "infobox")) {
				newInfobox(t);
				// System.out.println(t);
				builder = null;
			} else {
				builder = null;
				iterate(t);
			}

		}
	}

	public void visit(WtText n) {
		if (builder != null) {
			builder.append(n.getContent());
		}
		iterate(n);
	}

	private void newInfobox(WtNode node) {
		++infoboxDepth;
		if (infoboxDepth == 1)
			nodes.add(node);
		--infoboxDepth;
	}
}