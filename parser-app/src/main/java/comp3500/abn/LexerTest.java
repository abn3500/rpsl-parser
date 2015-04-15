package comp3500.abn;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.input.ReaderInputStream;

import net.ripe.db.whois.common.generated.ImportLexer;

public class LexerTest {
	private static String input = "import:   from AS4746\n" + 
			"          action pref=100;\n" + 
			"          accept AS7569 AS4776 AS7570 AS7572 AS4738 AS7571 AS7574 AS7573 AS1221 AS7474 AS4775"; 
	
	public static void main(String args[]) throws IOException, ClassNotFoundException {
		Reader in = new StringReader(input);
		
		Lexer lexer = new Lexer("import", in);
		System.out.println("Input: " + input);
		System.out.println("States: " + lexer.stateTable);
		System.out.println("Match Table: " + lexer.parse());
	}
}
