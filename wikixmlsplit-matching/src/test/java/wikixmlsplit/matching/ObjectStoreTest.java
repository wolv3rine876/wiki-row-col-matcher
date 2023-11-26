package wikixmlsplit.matching;

import org.junit.Test;
import wikixmlsplit.matching.data.RevisionData;
import wikixmlsplit.matching.data.TrackedObject;
import wikixmlsplit.matching.position.SimplePosition;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ObjectStoreTest {

	private static RevisionData r1 = new RevisionData() {

		@Override
		public Instant getInstant() {
			return Instant.ofEpochSecond(1);
		}
		
		@Override
		public BigInteger getId() {
			return BigInteger.valueOf(1);
		}

	};
	private static RevisionData r2 = new RevisionData() {
		@Override
		public Instant getInstant() {
			return Instant.ofEpochSecond(2);
		}

		@Override
		public BigInteger getId() {
			return BigInteger.valueOf(2);
		}
	};

	@Test
	public void testFreshStore() {
		ObjectStore<Object> store = new ObjectStore<>();

		List<Object> input = new ArrayList<>();
		input.add(new Object());
		input.add(new Object());
		List<TrackedObject<Object>> objects = new ArrayList<>();
		store.handleNewRevision(input, objects::add, r1, (obj, pos) -> new SimplePosition(pos) {});

		assertEquals(objects.size(), 2);
		for (TrackedObject<Object> o : objects) {
			assertTrue(o.isActive());
			assertNull(o.getPrevObject());
		}
	}

	@Test
	public void testDeletion() {
		ObjectStore<Object> store = new ObjectStore<>();

		List<Object> input = new ArrayList<>();
		input.add(new Object());
		input.add(new Object());
		store.handleNewRevision(input, i -> {
		}, r1, (obj, pos) -> new SimplePosition(pos) {});
		List<TrackedObject<Object>> objects = new ArrayList<>();
		store.handleNewRevision(new ArrayList<>(), objects::add, r2);

		assertEquals(objects.size(), 2);
		for (TrackedObject<Object> o : objects) {
			assertFalse(o.isActive());
			assertNotNull(o.getPrevObject());
		}
	}
	
	
}
