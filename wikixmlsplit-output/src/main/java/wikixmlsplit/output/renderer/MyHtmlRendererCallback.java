package wikixmlsplit.output.renderer;

import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.output.HtmlRendererCallback;
import org.sweble.wikitext.engine.output.MediaInfo;
import org.sweble.wikitext.engine.utils.UrlEncoding;
import org.sweble.wikitext.parser.nodes.WtUrl;

public final class MyHtmlRendererCallback implements HtmlRendererCallback {
	protected static final String LOCAL_URL = "http://en.wikipedia.org/wiki";
	
	@Override
	public boolean resourceExists(PageTitle target) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String makeUrl(PageTitle target) {
		String page = UrlEncoding.WIKI.encode(target.getNormalizedFullTitle());
		String f = target.getFragment();
		String url = page;
		if (f != null && !f.isEmpty())
			url = page + "#" + UrlEncoding.WIKI.encode(f);
		return LOCAL_URL + "/" + url;
	}

	@Override
	public String makeUrl(WtUrl target) {
		if (target.getProtocol().isEmpty())
			return target.getPath();
		return target.getProtocol() + ":" + target.getPath();
	}

	@Override
	public String makeUrlMissingTarget(String path) {
		return LOCAL_URL + "?title=" + path + "";

	}

	@Override
	public MediaInfo getMediaInfo(String title, int width, int height) {
		// TODO Auto-generated method stub
		return null;
	}
}