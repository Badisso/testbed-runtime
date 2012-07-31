package de.uniluebeck.itm.tr.iwsn;

import org.junit.Test;

public class NodeUrnTest {

	@Test
	public void testThatNodeUrnWithHexSuffixWorks() throws Exception {
		new NodeUrn("urn:wisebed:uzl1:0x1234");
		new NodeUrn("urn:wisebed:0x1234");
	}

	@Test
	public void testThatNodeUrnWithDecSuffixWorks() throws Exception {
		new NodeUrn("urn:wisebed:uzl1:1234");
		new NodeUrn("urn:wisebed:1234");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNodeUrnWithoutNamespaceIdentifierThrowsAnException() throws Exception {
		new NodeUrn("urn:1234");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNodeUrnWithoutUrnPrefixThrowsAnException() throws Exception {
		new NodeUrn("wisebed:uzl1:0x1234");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNodeUrnWithoutMacSuffixThrowsAnException() throws Exception {
		new NodeUrn("urn:wisebed:uzl1");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNodeUrnPrefixShouldThrowAnException() throws Exception {
		new NodeUrn("urn:wisebed:uzl1:");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatEmptyStringThrowsAnException() throws Exception {
		new NodeUrn("");
	}

	@Test(expected = NullPointerException.class)
	public void testThatNullThrowsAnException() throws Exception {
		new NodeUrn(null);
	}
}
