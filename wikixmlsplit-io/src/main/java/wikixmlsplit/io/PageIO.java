package wikixmlsplit.io;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;

import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;
import wikixmlsplit.datastructures.MyPageType;

import java.io.*;
import java.nio.file.Path;

public class PageIO {
	private Kryo kryo;
	
	public PageIO() {
		this(false);
	}

	public PageIO(boolean versioning) {
		this.kryo = new Kryo();
		if(versioning) {
			this.kryo.setDefaultSerializer(VersionFieldSerializer.class);
		}
	}

	public MyPageType read(Path inputPath) throws IOException {
		File dumpFile = inputPath.toFile();

		synchronized(kryo) {
			try (Input input = new Input(new SnappyInputStream(new FileInputStream(dumpFile)))) {
				return kryo.readObject(input, MyPageType.class);
			}
		}
	}

	public void write(Path outputPath, MyPageType page) throws FileNotFoundException {
		synchronized(kryo) {
			try (Output output = new Output(new SnappyOutputStream(new FileOutputStream(outputPath.toFile())))) {
				kryo.writeObject(output, page);
			}
		}
	}

}
