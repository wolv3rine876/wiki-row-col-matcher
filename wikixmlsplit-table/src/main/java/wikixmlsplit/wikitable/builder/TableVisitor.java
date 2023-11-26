package wikixmlsplit.wikitable.builder;

import de.fau.cs.osr.ptk.common.AstVisitor;
import org.sweble.wikitext.engine.nodes.EngNowiki;
import org.sweble.wikitext.engine.nodes.EngPage;
import org.sweble.wikitext.parser.nodes.*;


public class TableVisitor extends AstVisitor<WtNode> {

	private int tableDepth;
	private TableFactory table;
	private boolean useSpans;
	private int rowLimit;

	public TableVisitor(boolean useSpans) {
		this(useSpans, -1);
	}
	
	// =========================================================================

	public TableVisitor(boolean useSpans, int rowLimit) {
		this.useSpans = useSpans;
		this.rowLimit = rowLimit;
	}

	@Override
	protected WtNode before(WtNode node) {
		// This method is called by go() before visitation starts
		tableDepth = 0;
		return super.before(node);
	}

	@Override
	protected Object after(WtNode node, Object result) {

		// This method is called by go() after visitation has finished
		// The return value will be passed to go() which passes it to the caller
		return table.getOutput(useSpans);
	}

	// =========================================================================

	public void visit(WtNode n) {
		// Fallback for all nodes that are not explicitly handled below
		System.out.println(""
				+ " <" + n.getNodeName() + ">");
	}
	
	public void visit(WtXmlEndTag n) {
		if (table != null) {
			table.write("</");
			iterate(n);
			table.write(">");
		}
	}
	
	public void visit(EngNowiki w) {
		if(table != null) table.write(w.getContent());
	}

	public void visit(WtNodeList n) {
		iterate(n);
	}

	public void visit(WtUnorderedList e) {
		iterate(e);
	}

	public void visit(WtOrderedList e) {
		iterate(e);
	}

	public void visit(WtListItem item) {
		table.newline(1);
		iterate(item);
	}

	public void visit(EngPage p) {
		iterate(p);
	}

	public void visit(WtText text) {
		if (table != null)
			table.write(text.getContent());
	}

	public void visit(WtWhitespace w) {
		if (table != null)
			table.write(" ");
	}

	public void visit(WtBold b) {
		if (table == null)
			return;

		table.write("'''");
		iterate(b);
		table.write("'''");
	}

	public void visit(WtItalics i) {
		if (table == null)
			return;

		table.write("''");
		iterate(i);
		table.write("''");
	}

	public void visit(WtSection s) {
		iterate(s.getBody());
	}

	public void visit(WtXmlCharRef cr) {
		if (table != null)
			table.write(Character.toChars(cr.getCodePoint()));
	}

	public void visit(WtXmlEntityRef er) {
		if (table == null)
			return;

		table.write('&');
		table.write(er.getName());
		table.write(';');
	}

	public void visit(WtUrl url) {
		if (table == null)
			return;

		table.write(url.getProtocol());
		table.write(':');
		table.write(url.getPath());
	}

	public void visit(WtExternalLink link) {
		if (table == null)
			return;

		table.write('[');
		iterate(link);
		table.write(']');
	}

	public void visit(WtImageLink link) {
		if (table == null)
			return;

		table.write("[");
		// table.write(link.get());
		iterate(link);
		table.write(']');
	}

	public void visit(WtLinkTitle link) {
		if (!link.isEmpty()) {
			table.write("|");

			iterate(link);
		}
	}

	public void visit(WtLinkOptionKeyword link) {
		if (!link.getKeyword().isEmpty()) {
			table.write("|");
			table.write(link.getKeyword());
		}
	}

	public void visit(WtLinkOptionResize link) {
		if (!link.isEmpty()) {
			table.write("|");
			iterate(link);
		}
	}

	public void visit(WtLinkOptionGarbage link) {
	}

	public void visit(WtLinkOptionLinkTarget target) {

	}

	public void visit(WtLinkOptionAltText text) {

	}

	public void visit(WtInternalLink link) {
		table.write("[[");
		table.write(link.getPrefix());
		iterate(link);
		table.write(link.getPostfix());
		table.write("]]");
	}

	public void visit(WtParagraph p) {
		iterate(p);
		if (table != null)
			table.write("\n");
	}

	public void visit(WtHorizontalRule hr) {
		if (table != null) {
			table.write("\n ---- \n");
		}
	}

	public void visit(WtDefinitionList list) {
		iterate(list);
	}

	public void visit(WtDefinitionListDef list) {
		iterate(list);
	}

	public void visit(WtDefinitionListTerm list) {
		iterate(list);
	}

	public void visit(WtSemiPre pre) {
		iterate(pre);
	}

	public void visit(WtSemiPreLine pre) {
		iterate(pre);
	}

	/*
	 * ========================= TABLES
	 */

	public void visit(WtXmlElement e) {
		switch (e.getName().toLowerCase()) {
		case "br":
			if (table != null)
				table.newline(1);
			break;
		case "table":
			newTable(e.getBody());
			break;
		case "th":
			newHeader(e.getBody(), e.getXmlAttributes());
			break;
		case "td":
			newCell(e.getBody(), e.getXmlAttributes());
			break;
		case "tr":
			newRow(e.getBody());
			break;
		case "caption":
			newCaption(e.getBody());
			break;
		default:
			iterate(e.getBody());
		}

	}

	public void visit(WtTableImplicitTableBody t) {

		iterate(t.getBody());
	}

	public void visit(WtTable t) {
		newTable(t.getBody());
	}

	public void visit(WtTableHeader t) {
		newHeader(t.getBody(), t.getXmlAttributes());
	}

	public void visit(WtTableRow t) {
		boolean cellsDefined = false;
		for (WtNode cell : t.getBody())
		{
			switch (cell.getNodeType())
			{
				case WtNode.NT_TABLE_CELL:
				case WtNode.NT_TABLE_HEADER:
					cellsDefined = true;
					break;
			}
		}
		
		if(cellsDefined)
			newRow(t.getBody());
	}

	public void visit(WtTableCell t) {
		newCell(t.getBody(), t.getXmlAttributes());
	}

	public void visit(WtTableCaption t) {
		newCaption(t.getBody());
	}

	private void newTable(WtBody body) {
		++tableDepth;
		if (tableDepth == 1)
			table = new TableFactory();
		iterate(body);
		--tableDepth;
		if (tableDepth == 0) {
        }
	}

	private void newHeader(WtBody body, WtXmlAttributes wtXmlAttributes) {
		if (table == null)
			return;

		if (tableDepth == 1)
			table.newHeader();
		handleCellAttributes(wtXmlAttributes);
		iterate(body);
		if (tableDepth == 1)
			table.finishHeader();
	}

	private void newRow(WtBody body) {
		if (table == null)
			return;
		if(rowLimit > 0 && table.getRowCount() >= rowLimit)
			return;
		if (tableDepth == 1)
			table.newRow();
		iterate(body);
		
		if (tableDepth == 1)
			table.finishRow();
	}

	private void newCell(WtBody body, WtXmlAttributes wtXmlAttributes) {
		if (table == null)
			return;
		if (tableDepth == 1)
			table.newCell();
		handleCellAttributes(wtXmlAttributes);
		iterate(body);
		if (tableDepth == 1)
			table.finishCell();
	}

	private void handleCellAttributes(WtXmlAttributes wtXmlAttributes) {
		if (wtXmlAttributes != null && !wtXmlAttributes.isEmpty()) {
			for (WtNode n : wtXmlAttributes) {
				if (n instanceof WtXmlAttribute) {
					// System.out.println(n);
					WtXmlAttribute attr = (WtXmlAttribute) n;
					if (attr.getName().isResolved() && attr.getValue().size() > 0) {
						String propName = attr.getName().getAsString().toLowerCase();
						switch (propName) {
						case "align":
						case "width":
						case "style":
						case "valign":
						case "bgcolor":
						case "height":
							// ignore
							break;
						case "rowspan": {
							WtNode content = attr.getValue().get(0);
							if(content instanceof WtText) {
								try {
									int value = Integer.parseInt(((WtText)content).getContent());
									table.setRowSpan(value);
								}
								catch(NumberFormatException ignored) {
									
								}
							}
							break;
						}
						case "colspan": {
							WtNode content = attr.getValue().get(0);
							if(content instanceof WtText) {
								try {
									int value = Integer.parseInt(((WtText)content).getContent());
									table.setColSpan(value);
								}
								catch(NumberFormatException ignored) {
									
								}
							}
						
							break;
						}
						case "scope": {
							
							break;
						}
						default:
                        }
					} else {
					}

				}
			}
		}
	}

	private void newCaption(WtBody body) {
		if (table == null)
			return;
		if (tableDepth == 1)
			table.newCaption();
		iterate(body);
		if (tableDepth == 1)
			table.finishCaption();
	}

	public void visit(WtSignature sig) {

	}

	public void visit(WtRedirect n) {
	}

	public void visit(WtIllegalCodePoint n) {
	}

	public void visit(WtXmlComment n) {
	}

	public void visit(WtTemplate n) {
		if (table == null)
			return;
		table.write("{{");
		iterate(n.getName());
		for (WtNode i : n.getArgs()) {
			WtTemplateArgument arg = (WtTemplateArgument) i;
			table.write("|");

			if (arg.getName().isResolved()) {
				table.write(arg.getName().getAsString());
				table.write("=");
			}
			iterate(arg.getValue());
		}
		table.write("}}");
	}

	public void visit(WtTemplateArgument n) {
		if (table == null)
			return;
		if (n.hasName()) {
			iterate(n.getName());
			table.write("=");
		}
		iterate(n.getValue());
	}

	public void visit(WtTemplateParameter n) {
	}

	public void visit(WtTagExtension n) {
	}

	public void visit(WtPageSwitch n) {
	}

	// =========================================================================

}
