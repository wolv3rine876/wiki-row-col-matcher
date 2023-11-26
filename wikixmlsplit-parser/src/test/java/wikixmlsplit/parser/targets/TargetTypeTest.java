package wikixmlsplit.parser.targets;

import org.junit.Assert;
import org.junit.Test;

public class TargetTypeTest {

	@Test
	public void testListPattern() {
		Assert.assertTrue(TargetType.LISTS.accepts("tawetawt\n* Test"));
		Assert.assertTrue(TargetType.LISTS.accepts("tawetawt\n  * Test"));
		Assert.assertFalse(TargetType.LISTS.accepts("tawetawt\n tr * Test"));
		Assert.assertTrue(TargetType.LISTS.accepts("tawetawt\r\n  * Test"));
		Assert.assertTrue(TargetType.LISTS.accepts("tawetawt\r\n* Test"));

		Assert.assertTrue(TargetType.LISTS.accepts("tawetawt\r\n#Test"));
		Assert.assertTrue(TargetType.LISTS.accepts("tawetawt\r\n;Test"));
		Assert.assertTrue(TargetType.LISTS.accepts("tawetawt\r\n:Test"));

		Assert.assertFalse(TargetType.LISTS.accepts("tawetawt\r\n-Test"));

		Assert.assertTrue(TargetType.LISTS.accepts("tawetawt\r\n<ul>\nTest</ul>"));
	}
}
