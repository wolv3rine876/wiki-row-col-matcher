package wikixmlsplit.infobox.builder;

import de.fau.cs.osr.ptk.common.AstVisitor;
import org.sweble.wikitext.parser.nodes.*;
import wikixmlsplit.infobox.Infobox;

/**
 *
 */
public class InfoboxVisitor extends AstVisitor<WtNode> {

	private boolean firstTemplate;
	private Infobox infobox;
	private StringBuffer buffer;
	private String source;

	public InfoboxVisitor(String source) {
		this.source = source;
	}

	// =========================================================================

	@Override
	protected WtNode before(WtNode node) {
		// This method is called by go() before visitation starts
		firstTemplate = true;
		infobox = new Infobox();
		buffer = new StringBuffer();
		return super.before(node);
	}

	@Override
	protected Object after(WtNode node, Object result) {

		// This method is called by go() after visitation has finished
		// The return value will be passed to go() which passes it to the caller
		return infobox;
	}

	// =========================================================================

	public void visit(WtNode n) {
		System.err.println("Unhandled" + n.getNodeName());
		System.out.println(n);
		System.out.println(source);
		iterate(n);
	}

	// =========================================================================

	public void visit(WtXmlComment n) {
		// Hide those...
	}

	public void visit(WtIgnored n) {
		// Well, ignore it ...
	}

	// TEMPLATES

	public void visit(WtTemplate n) {
		if (firstTemplate) {
			firstTemplate = false;
			buffer.setLength(0);
			iterate(n.getName());
			infobox.setTemplate(buffer.toString().trim());
			WtTemplateArguments arguments = n.getArgs();
			int pId = 0;
			for (WtNode i : arguments) {
				WtTemplateArgument arg = (WtTemplateArgument) i;

				String key;
				boolean noName = false;
				if (arg.getName().isResolved()) {
					key = arg.getName().getAsString();
				} else {
					if (arg.getName().equals(WtName.NO_NAME)) {
						key = "" + pId;
						noName = true;
					} else {
						buffer.setLength(0);
						iterate(arg.getName());
						key = buffer.toString();
					}

				}
				++pId;

				WtValue val = arg.getValue();
				String valString;
				if (val.size() == 1 && val.get(0).isNodeType(WtNode.NT_TEXT)) {
					valString = ((WtText) val.get(0)).getContent().trim();
				} else {
					buffer.setLength(0);
					iterate(val);
					valString = buffer.toString().trim();
				}
				if(!noName || !valString.isEmpty())
					infobox.addAttribute(key, valString);
			}

		} else {
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
		System.err.println("unexpected xml attributes");
		System.out.println(n);
		System.out.println(source);
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

	// WtName
	// WtValue

}
