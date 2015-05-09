package comp3500.abn.rpsl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class AttributeLexerWrapperTest {
	private static final String importString = 
			"    import: from AS2 7.7.7.2 at 7.7.7.1 action pref = 1;\n" + 
			"            from AS2                    action pref = 2;\n" + 
			"            accept AS4";

	@Test
	public void loadsExistingLexer() {
		try {
			new AttributeLexerWrapper("import");
		} catch (ClassNotFoundException e) {
			fail("Failed to instantiate known lexer: import");
		}
	}
	
	@Test
	public void parsesImportBlock() throws ClassNotFoundException, IOException{
		AttributeLexerWrapper importLexer = new AttributeLexerWrapper("import");

		StringReader strReader = new StringReader(importString);
		List<Pair<String,List<String>>> ast = importLexer.parse(strReader);
		assertEquals("Parser genereated abmormal output",
				"[(from,[AS2, 7.7.7.2]), (at,[7.7.7.1]), (action,[pref, =, 1]), (from,[AS2]), (action,[pref, =, 2]), (accept,[AS4])]",
				ast.toString());		
	}
	
	@Test
	public void parsesImportBlockObject() {
		RpslAttribute attr = new RpslAttribute(AttributeType.IMPORT, importString);
		assertEquals("Parser genereated abmormal output",
				"[(from,[AS2, 7.7.7.2]), (at,[7.7.7.1]), (action,[pref, =, 1]), (from,[AS2]), (action,[pref, =, 2]), (accept,[AS4])]",
				AttributeLexerWrapper.parse(attr).toString());	
	}
	
	@Test
	public void noEmptyEntries() {
		//Empty entries should be filtered. all maps originally have an empty entry for their key (ie import: ). check that it is missing
		RpslAttribute attr = new RpslAttribute(AttributeType.IMPORT, importString);
		List<Pair<String, List<String>>> ast = AttributeLexerWrapper.parse(attr);
		for(Pair<String, List<String>> entry : ast) {
			if(entry.getRight() == null || entry.getRight().size() == 0)
				fail("Empty entry found in parser output: " + entry.getLeft());
		}
	}
	
	@Test
	public void capturesOpeningToken() {
		//Make sure that attributes with opening token are captured. used ifaddr as test
		RpslAttribute attr = new RpslAttribute(AttributeType.IFADDR, "ifaddr:   193.0.0.158   masklen 27");
		List<Pair<String, List<String>>> ast = AttributeLexerWrapper.parse(attr);
		assertEquals("Key of first entry should be state name", "dns", ast.get(0).getLeft());
		assertEquals("First entry should be captured when token", "193.0.0.158", ast.get(0).getRight().get(0));
		assertTrue("Opening with token doesn't inadvertently capture rest of attribute", ast.get(0).getRight().size() == 1);
	}
}
