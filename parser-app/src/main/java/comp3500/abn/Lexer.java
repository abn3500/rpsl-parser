package comp3500.abn;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.WordUtils;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder.ListMultimapBuilder;

import net.ripe.db.whois.common.rpsl.AttributeLexer;
import net.ripe.db.whois.common.rpsl.AttributeParser;

public class Lexer {
	
	private AttributeLexer lexer;
	Map<Integer, String> stateTable;
	
	public Lexer(String fieldName, Reader in) throws ClassNotFoundException {
		lexer = loadLexerInstance(fieldName, in);
		stateTable = generateStateTable(fieldName);
	}
	
	public Lexer(String fieldName, InputStream in) throws ClassNotFoundException {
		this(fieldName, new InputStreamReader(in));
	}
	
	private AttributeLexer loadLexerInstance(String fieldName, Reader in) throws ClassNotFoundException {
		String lexerClassName = "net.ripe.db.whois.common.generated." + WordUtils.capitalize(fieldName) + "Lexer";
		Class<AttributeLexer> lexerClass =  (Class<AttributeLexer>) Class.forName(lexerClassName);
		
		try {
			Constructor<AttributeLexer> lexerConstructor = lexerClass.getConstructor(Reader.class);
			return lexerConstructor.newInstance(in);
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw (new ClassNotFoundException(
					lexerClass.getName() + " is not a valid AttributeParser", e));
		}
	}
	
	private Map<Integer, String> generateStateTable(String fieldName) throws ClassNotFoundException {
		String parserClassName = "net.ripe.db.whois.common.generated." + WordUtils.capitalize(fieldName) + "Parser";
		Class<AttributeLexer> parserClass =  (Class<AttributeLexer>) Class.forName(parserClassName);
		Map<Integer, String> stateTable = new HashMap<Integer, String>();
		
		for(Field field : parserClass.getDeclaredFields()) {
			int modifierMask = Modifier.PUBLIC | Modifier.FINAL | Modifier.STATIC;
			
			//TODO regex match on field name for \w_\w?
			if(field.getModifiers() == modifierMask && field.getType() == short.class) {
				try {
					stateTable.put(new Integer(field.getShort(null)), field.getName());
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return stateTable;
	}
	
	public ListMultimap<String, String> parse() {
		//TODO yyreset()
		int lexerState = 0;
		ListMultimap<String, String> matchTable = LinkedListMultimap.create(stateTable.size());
		
		do {
			try {
				lexerState = lexer.yylex();
			} catch (IOException e) {
				// TODO handle IO failure properly
				break;
			}
			if(lexer.yylength() > 0) {
				matchTable.put(stateTable.get(lexerState), lexer.yytext());
			}
		} while (lexerState > 0);
		return matchTable;
	}
}
