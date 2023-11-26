package wikixmlsplit.webtable.io;

import wikixmlsplit.matching.data.RevisionData;

import java.math.BigInteger;
import java.time.Instant;

public class WebpageRevision implements RevisionData {



	private String url;
	private BigInteger id;
	private Instant date;

	public WebpageRevision(String url, BigInteger id, Instant date) {
		super();
		this.url = url;
		this.id = id;
		this.date = date;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public Instant getInstant() {
		return date;
	}

	@Override
	public BigInteger getId() {
		return id;
	}

	
	@Override
	public String toString() {
		return "WebpageRevision [url=" + url + ", id=" + id + ", date=" + date + "]";
	}


}
