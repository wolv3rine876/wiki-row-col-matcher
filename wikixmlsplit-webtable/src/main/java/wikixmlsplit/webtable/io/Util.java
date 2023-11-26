package wikixmlsplit.webtable.io;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@SuppressWarnings("deprecation")
public class Util {

	protected static final Safelist whitelist = Safelist.simpleText();
	protected static final CharMatcher cleaner = CharMatcher.whitespace();
	
	public static String cleanCell(String cell) {
		cell = Jsoup.clean(cell, whitelist);
		cell = StringEscapeUtils.unescapeHtml4(cell);
		cell = cleaner.trimAndCollapseFrom(cell, ' ');
		return cell;
	}
	
	public static String makeSafeFileName(String s) {
		String safe = s.replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_\\-&]+", "");
		return safe.substring(0, Math.min(60, safe.length()));
	}
}
