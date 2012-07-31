package de.uniluebeck.itm.tr.iwsn;

import org.junit.Test;

public class NodeUrnPrefixTest {

	@Test
	public void testThatWisebedPrefixWorks() throws Exception {
		new NodeUrnPrefix("urn:wisebed:uzl1:");
	}

	@Test
	public void testThatUrnPrefixWithOnlyNamespaceIdentifierWorks() throws Exception {
		new NodeUrnPrefix("urn:wisebed:");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatWisebedPrefixWithoutColonThrowsAnException1() throws Exception {
		new NodeUrnPrefix("urn:wisebed:uzl1");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatWisebedPrefixWithoutColonThrowsAnException2() throws Exception {
		new NodeUrnPrefix("urn:wisebed");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNodeUrnPrefixWithHexSuffixThrowsAnException() throws Exception {
		new NodeUrnPrefix("urn:wisebed:uzl1:0x1234");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNodeUrnPrefixWithDecSuffixThrowsAnException() throws Exception {
		new NodeUrnPrefix("urn:wisebed:uzl1:1234");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatNodeUrnPrefixWithoutNamespaceIdentifierThrowsAnException() throws Exception {
		new NodeUrnPrefix("urn:");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testThatEmptyStringThrowsAnException() throws Exception {
		new NodeUrnPrefix("");
	}

	@Test(expected = NullPointerException.class)
	public void testThatNullThrowsAnException() throws Exception {
		new NodeUrnPrefix(null);
	}
}
