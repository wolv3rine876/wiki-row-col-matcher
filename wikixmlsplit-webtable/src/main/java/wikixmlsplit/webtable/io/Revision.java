package wikixmlsplit.webtable.io;

import org.jsoup.nodes.Document;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Revision {

	private List<String> metadata;

	private Document content;

	public Revision(List<String> metadata, Document content) {
		super();
		this.metadata = metadata;
		this.content = content;
	}

	public List<String> getMetadata() {
		return metadata;
	}

	public Document getContent() {
		return content;
	}

	public BigInteger getId() {
		return new BigInteger(metadata.get(1));
	}

	public Date getDate() {
		try {
			return new SimpleDateFormat("yyyyMMddHHmmss").parse(metadata.get(1));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
