package wikixmlsplit.output.renderer;

import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.output.SafeLinkTitlePrinter;
import org.sweble.wikitext.parser.nodes.*;

import java.io.Writer;

public class MySafeLinkTitlePrinter extends SafeLinkTitlePrinter {

	public MySafeLinkTitlePrinter(Writer writer, WikiConfig wikiConfig) {
		super(writer, wikiConfig);
		// TODO Auto-generated constructor stub
	}

	public void visit(WtExternalLink n)
	{
	}
	
	public void visit(WtImageLink n)
	{
	}
	
	@Override
	public void visit(WtRedirect n)
	{
	}
	
	
	public void visit(WtTable n)
	{
	}
	
	@Override
	public void visit(WtTableCaption n)
	{
	}
	
	@Override
	public void visit(WtTableCell n)
	{
	}
	
	@Override
	public void visit(WtTableHeader n)
	{
	}
	
	@Override
	public void visit(WtTableRow n)
	{
	}
	
	@Override
	public void visit(WtUrl n)
	{
	}
	@Override
	public void visit(WtXmlAttribute n)
	{
	}
	
	
	
}
