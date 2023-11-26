package wikixmlsplit.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class InstantTypeAdapter extends TypeAdapter<Instant> {
	@Override
	public void write(JsonWriter out, Instant value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(DateTimeFormatter.ISO_INSTANT.format(value));
		}
	}

	@Override
	public Instant read(JsonReader jsonReader) throws IOException {
		if (jsonReader.peek() == JsonToken.NULL) {
			jsonReader.nextNull();
			return null;
		} else {
			return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(jsonReader.nextString()));
		}
	}
}