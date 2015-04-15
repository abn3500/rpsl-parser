package comp3500.abn;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.input.ReaderInputStream;

import comp3500.abn.rpsl.AttributeLexerWrapper;

import net.ripe.db.whois.common.generated.ImportLexer;

public class LexerTest {
	private static String input = "    import: from AS2 7.7.7.2 at 7.7.7.1 action pref = 1; action pref = 1.5;\n" + 
			"            from AS2                    action pref = 2;\n" + 
			"            accept AS4"; 
	
	public static void main(String args[]) throws IOException, ClassNotFoundException {
		Reader in = new StringReader(input),n = null;
		
		AttributeLexerWrapper lexer = new AttributeLexerWrapper("import");
		System.out.println("Input: " + input);
		System.out.println("Match Table (context): " + lexer.parse(in));
	}
}
