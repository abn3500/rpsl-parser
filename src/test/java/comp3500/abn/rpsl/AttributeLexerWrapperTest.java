package comp3500.abn.rpsl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class AttributeLexerWrapperTest {

	@Test
	public void loadsExistingLexer() {
		try {
			AttributeLexerWrapper a = new AttributeLexerWrapper("import");
		} catch (ClassNotFoundException e) {
			fail("Failed to instantiate known lexer: import");
		}
	}
	
	@Test
	public void parsesImportBlock() throws ClassNotFoundException, IOException{
		AttributeLexerWrapper importLexer = new AttributeLexerWrapper("import");
		String importString = "    import: from AS2 7.7.7.2 at 7.7.7.1 action pref = 1;\n" + 
				"            from AS2                    action pref = 2;\n" + 
				"            accept AS4";
		StringReader strReader = new StringReader(importString);
		List<Pair<String,List<String>>> ast = importLexer.parse(strReader);
		assertEquals("Parser genereated abmormal output",
				"[(from,[AS2, 7.7.7.2]), (at,[7.7.7.1]), (action,[pref, =, 1]), (from,[AS2]), (action,[pref, =, 2]), (accept,[AS4])]",
				ast.toString());		
	}

}
