package wikixmlsplit.lists.builder;

import de.fau.cs.osr.ptk.common.AstVisitor;
import org.sweble.wikitext.parser.nodes.*;
import wikixmlsplit.lists.WikiList;

/**
 *
 */
public class WikiListVisitor extends AstVisitor<WtNode> {

	private WikiList list;
	private StringBuffer buffer;
	private String source;
	private boolean inside = false;

	public WikiListVisitor(String source) {
		this.source = source;
	}

	// =========================================================================

	@Override
	protected WtNode before(WtNode node) {
		// This method is called by go() before visitation starts
		list = new WikiList();
		buffer = new StringBuffer();
		return super.before(node);
	}

	@Override
	protected Object after(WtNode node, Object result) {

		// This method is called by go() after visitation has finished
		// The return value will be passed to go() which passes it to the caller
		return list;
	}

	// =========================================================================

	public void visit(WtNode n) {
		System.err.println("Unhandled" + n.getNodeName());
		System.out.println(n);
		System.out.println(source);
		iterate(n);
	}
	

	public void visit(WtUnorderedList e) {
		iterate(e);
	}

	public void visit(WtOrderedList e) {
		iterate(e);
	}
	
	public void visit(WtPageName e) {
		iterate(e);
	}
	
	public void visit(WtDefinitionList n) {
		iterate(n);
	}

	public void visit(WtBody n) {
		iterate(n);
	}

	public void visit(WtDefinitionListDef n) {
		newListItem(n);
	}

	public void visit(WtDefinitionListTerm n) {
		newListItem(n);
	}

	public void visit(WtListItem item) {
		newListItem(item);
	}

	public void visit(WtXmlElement e) {
		switch (e.getName().toLowerCase()) {
		case "li": {
			newListItem(e);
			break;
		}
		default: {
			iterate(e);
		}
		}

	}

	// =========================================================================

	private void newListItem(WtNode e) {
		if(inside) {
			iterate(e);
		} else {
			inside = true;
			buffer.setLength(0);
			iterate(e);
			list.addItem(buffer.toString());
			inside = false;
		}
	}

	public void visit(WtXmlComment n) {
		// Hide those...
	}

	public void visit(WtIgnored n) {
		// Well, ignore it ...
	}

	// TEMPLATES

	public void visit(WtTemplate n) {

		buffer.append("{{");
		iterate(n.getName());
		for (WtNode i : n.getArgs()) {
			WtTemplateArgument arg = (WtTemplateArgument) i;
			buffer.append("|");

			if (arg.getName().isResolved()) {
				buffer.append(arg.getName().getAsString());
				buffer.append("=");
			}
			iterate(arg.getValue());
		}
		buffer.append("}}");

	}

	public void visit(WtTemplateArgument n) {
		System.err.println("Unexpected WtTemplateArgument");
		System.out.println(n);
		System.out.println(source);
	}

	public void visit(WtTemplateArguments n) {
		System.err.println("Unexpected WtTemplateArguments");
		System.out.println(n);
		System.out.println(source);
	}

	public void visit(WtTemplateParameter n) {
		buffer.append("{{{");
		iterate(n.getName());
		if (!n.getDefault().equals(WtValue.NO_VALUE)) {
			buffer.append("|");
			iterate(n.getDefault());
		}
		buffer.append("}}}");
	}

	public void visit(WtText n) {
		buffer.append(n.getContent());
	}

	public void visit(WtTagExtension n) {
		buffer.append("<");
		buffer.append(n.getName());
		iterate(n.getXmlAttributes());
		buffer.append(">");
		buffer.append(n.getBody().getContent());
		buffer.append("</");
		buffer.append(n.getName());
		buffer.append(">");
	}

	public void visit(WtXmlAttributes n) {
		iterate(n);
	}

	public void visit(WtXmlAttribute n) {
		buffer.append(" ");
		iterate(n.getName());
		buffer.append("=");
		iterate(n.getValue());
	}

	public void visit(WtXmlAttributeGarbage n) {
		// ignore
	}

	public void visit(WtXmlEntityRef n) {
		buffer.append("&");
		buffer.append(n.getName());
		buffer.append(";");
	}

	public void visit(WtXmlCharRef n) {
		buffer.append("&");
		buffer.append("#");
		buffer.append(n.getCodePoint());
		buffer.append(";");
	}

	public void visit(WtRedirect n) {
	}

	public void visit(WtIllegalCodePoint n) {
	}

	public void visit(WtPageSwitch n) {
	}

	public void visit(WtWhitespace w) {
		buffer.append(" ");
	}

	public void visit(WtBold b) {

		buffer.append("'''");
		iterate(b);
		buffer.append("'''");
	}

	public void visit(WtItalics i) {
		buffer.append("''");
		iterate(i);
		buffer.append("''");
	}

	public void visit(WtSection s) {
		iterate(s.getBody());
	}

	public void visit(WtUrl url) {

		buffer.append(url.getProtocol());
		buffer.append(':');
		buffer.append(url.getPath());
	}

	public void visit(WtExternalLink link) {
		buffer.append('[');
		iterate(link);
		buffer.append(']');
	}

	public void visit(WtImageLink link) {

		buffer.append("[");
		// buffer.append(link.get());
		iterate(link);
		buffer.append(']');
	}

	public void visit(WtLinkTitle link) {
		if (!link.isEmpty()) {
			buffer.append("|");

			iterate(link);
		}
	}

	public void visit(WtLinkOptionKeyword link) {
		if (!link.getKeyword().isEmpty()) {
			buffer.append("|");
			buffer.append(link.getKeyword());
		}
	}

	public void visit(WtLinkOptionResize link) {
		if (!link.isEmpty()) {
			buffer.append("|");
			iterate(link);
		}
	}

	public void visit(WtInternalLink link) {
		buffer.append("[[");
		buffer.append(link.getPrefix());
		iterate(link);
		buffer.append(link.getPostfix());
		buffer.append("]]");
	}

	public void visit(WtParagraph p) {
		iterate(p);
		buffer.append("\n");
	}

	public void visit(WtHorizontalRule hr) {
		buffer.append("\n ---- \n");
	}

	// WtName
	// WtValue

}
