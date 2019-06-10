import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.*;
import javax.swing.JFileChooser;

/**
 * The parser and interpreter. The top level parse function, a main method for
 * testing, and several utility methods are provided. You need to implement
 * parseProgram and all the rest of the parser.
 */
public class Parser {

	public static final String[] actions = new String[]{"move", "wait", "turnL", "turnR", "takeFuel", "turnAround", "shieldOn", "shieldOff"};
	public static final String[] operations = new String[]{"add", "sub", "mul", "div"};
	public static HashMap<String, Integer> variables = new HashMap<>();

	/**
	 * Top level parse method, called by the World
	 */
	static RobotProgramNode parseFile(File code) {
		Scanner scan = null;
		try {
			scan = new Scanner(code);

			// the only time tokens can be next to each other is
			// when one of them is one of (){},;
			scan.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");

			RobotProgramNode n = parseProgram(scan); // You need to implement this!!!

			scan.close();
			return n;
		} catch (FileNotFoundException e) {
			System.out.println("Robot program source file not found");
		} catch (ParserFailureException e) {
			System.out.println("Parser error:");
			System.out.println(e.getMessage());
			scan.close();
		}
		return null;
	}

	/** For testing the parser without requiring the world */

	public static void main(String[] args) {
		if (args.length > 0) {
			for (String arg : args) {
				File f = new File(arg);
				if (f.exists()) {
					System.out.println("Parsing '" + f + "'");
					RobotProgramNode prog = parseFile(f);
					System.out.println("Parsing completed ");
					if (prog != null) {
						System.out.println("================\nProgram:");
						System.out.println(prog);
					}
					System.out.println("=================");
				} else {
					System.out.println("Can't find file '" + f + "'");
				}
			}
		} else {
			while (true) {
				JFileChooser chooser = new JFileChooser(".");// System.getProperty("user.dir"));
				int res = chooser.showOpenDialog(null);
				if (res != JFileChooser.APPROVE_OPTION) {
					break;
				}
				RobotProgramNode prog = parseFile(chooser.getSelectedFile());
				System.out.println("Parsing completed");
				if (prog != null) {
					System.out.println("Program: \n" + prog);
				}
				System.out.println("=================");
			}
		}
		System.out.println("Done");
	}

	// Useful Patterns

	private static Pattern NUMPAT = Pattern.compile("-?\\d+"); // ("-?(0|[1-9][0-9]*)");
	private static Pattern OPENPAREN = Pattern.compile("\\(");
	private static Pattern CLOSEPAREN = Pattern.compile("\\)");
	private static Pattern OPENBRACE = Pattern.compile("\\{");
	private static Pattern CLOSEBRACE = Pattern.compile("}");
	private static Pattern COMMA = Pattern.compile(",");
	private static Pattern VAR = Pattern.compile("\\$[A-Za-z][A-Za-z0-9]*");
	private static Pattern RELOP = Pattern.compile("lt|gt|eq");
	private static Pattern OP = Pattern.compile("add|sub|mul|div");
	private static Pattern SEMICOLON = Pattern.compile(";");
	private static Pattern EQUALS = Pattern.compile("=");
	private static Pattern SENSOR = Pattern.compile("fuelLeft|oppLR|oppFB|numBarrels|barrelLR|barrelFB|wallDist");

	/**
	 * PROG ::= STMT+
	 */
	public static RobotProgramNode parseProgram(Scanner s) {
		ArrayList<StatementNode> buffer = new ArrayList<>(); //create local buffer for storing statements in a readable way
		while (s.hasNext()){
			buffer.add(parseLine(s));
		}
		return new StatementNode.programNode(buffer);
	}

	/** Chooses which parsing method to call after reading the next token.
	 *
	 *
	 * @param s - our scanner object
	 * @return the next line encapsulated in a StatementNode object
	 */
	private static StatementNode parseLine(Scanner s){

		StatementNode.loopNode loopNode = null;
		StatementNode.actionNode actionNode = null;
		StatementNode.whileNode whileNode = null;
		StatementNode.ifNode ifNode = null;

		if (s.hasNext("loop")){
			s.next();
			loopNode = parseLoop(s);
		}
		else if (s.hasNext("while")){
			s.next();
			whileNode = parseWhile(s);
		}
		else if (s.hasNext("if")){
			s.next();
			ifNode = parseIf(s);
		}
		else if (s.hasNext(VAR)){
			parseVariable(s);
		}
		else {
			actionNode = parseAction(s);
		}
		return new StatementNode(loopNode, actionNode, whileNode, ifNode);
	}

	private static void parseVariable(Scanner s){
		String varName = s.next();
		require(EQUALS, "Invalid syntax: = expected", s);
		int varValue = requireInt(NUMPAT, "Invalid syntax: integer expected", s);
		variables.put(varName, varValue);
		require(SEMICOLON, "Invalid syntax: ; expected", s);
	}

	private static StatementNode.loopNode parseLoop(Scanner s){
		return new StatementNode.loopNode(parseBlock(s));
	}

	private static StatementNode.whileNode parseWhile(Scanner s){
		require(OPENPAREN, "Invalid syntax: ( expected", s);
		StatementNode.condNode condition = parseCond(s);
		require(CLOSEPAREN, "Invalid syntax: ) expected", s);
		StatementNode.blockNode blockNode = parseBlock(s);
		return new StatementNode.whileNode(blockNode, condition);
	}

	private static StatementNode.ifNode parseIf(Scanner s){
		require(OPENPAREN, "Invalid syntax: ( expected", s);
		StatementNode.condNode condition = parseCond(s);
		require(CLOSEPAREN, "Invalid syntax: ) expected", s);
		StatementNode.elseNode elseNode = null;
		ArrayList<StatementNode.ifNode> elif = new ArrayList<>();
		StatementNode.blockNode blockNode = parseBlock(s);
		while (s.hasNext("elif")){
			s.next();
			elif.add(parseIf(s));
		}
		if (s.hasNext("else")){
			s.next();
			elseNode = parseElse(s);
		}
		return new StatementNode.ifNode(condition, blockNode, elseNode, elif);
	}

	private static StatementNode.elseNode parseElse(Scanner s){
		return new StatementNode.elseNode(parseBlock(s));
	}


	private static StatementNode.condNode parseCond(Scanner s){
		StatementNode.relopNode relopNode;
		StatementNode.condNode cond1;
		StatementNode.condNode cond2;


		if (s.hasNext(RELOP)){
			relopNode = parseRelop(s);
			return new StatementNode.condNode(relopNode, null, null, null);
		}
		else if (s.hasNext("and") || s.hasNext("or")){
			String operation = s.next();
			require(OPENPAREN, "Invalid syntax: ( expected", s);
			cond1 = parseCond(s);
			require(COMMA, "Invalid syntax: , expected", s);
			cond2 = parseCond(s);
			require(CLOSEPAREN, "Invalid syntax: ) expected", s);
			return new StatementNode.condNode(null, operation, cond1, cond2);
		}
		else if (s.hasNext("not")){
			String operation = s.next();
			require(OPENPAREN, "Invalid syntax: ( expected", s);
			cond1 = parseCond(s);
			require(CLOSEPAREN, "Invalid syntax: ) expected", s);
			return new StatementNode.condNode(null, operation, cond1, null);
		}
		else {
			fail("Invalid syntax: not a suitable condition", s);
			return null;
		}
	}


	private static StatementNode.actionNode parseAction(Scanner s){
		String action = s.next();
		StatementNode.argumentNode args = null;
		String[] array = Parser.actions;
		for (int i = 0; i < array.length; i++){
			if (array[i].equals(action)){
				if (s.hasNext(OPENPAREN)){
					if (action.equals(array[0]) || action.equals(array[1])) {
						s.next();
						args = parseArgs(s);
						require(CLOSEPAREN, "Invalid syntax: ) expected", s);
					}
					else fail("Invalid syntax: only move and wait take arguments", s);
				}
				require(SEMICOLON, "Invalid syntax: ; expected", s);
				return new StatementNode.actionNode(action, args);
			}
		}
		fail("Invalid syntax: statement expected", s);
		return null;
	}

	private static StatementNode.blockNode parseBlock(Scanner s){
		List<StatementNode> statements = new ArrayList<>();
		require(OPENBRACE, "Invalid syntax: { expected", s);
		while (s.hasNext()) {
			if (s.hasNext(CLOSEBRACE)){
				break;
			}
			statements.add(parseLine(s));
		}
		require(CLOSEBRACE, "Invalid syntax: } expected", s);
		if (statements.size() == 0){
			fail("Invalid syntax: statement expected", s);
		}
		return new StatementNode.blockNode(statements);
	}

	private static StatementNode.argumentNode parseArgs(Scanner s){
		Integer integer = null;
		StatementNode.opNode opNode = null;
		StatementNode.senNode senNode = null;
		if (s.hasNext(NUMPAT)){
			integer = s.nextInt();
		}
		else if (s.hasNext(OP)) {
			opNode = parseOp(s);
		}
		else if (s.hasNext(VAR)){
			String key = s.next();
			boolean foundKey = false;
			for (String string : variables.keySet()){
				if (s.equals(key)){
					foundKey = true;
					integer = variables.get(key);
				}
			}
			if (!foundKey){
				variables.put(key, 0);
				integer = 0;
			}
		}
		else senNode = parseSensor(s);
		return new StatementNode.argumentNode(integer, senNode, opNode);
	}

	private static StatementNode.senNode parseSensor(Scanner s){
		String sensorType = s.next();
		return new StatementNode.senNode(sensorType);
	}

	private static StatementNode.relopNode parseRelop(Scanner s){
		String type = s.next();
		require(OPENPAREN, "Invalid syntax: ( expected", s);
		StatementNode.argumentNode arg1 = parseArgs(s);
		require(COMMA, "Invalid syntax: , expected", s);
		StatementNode.argumentNode arg2 = parseArgs(s);
		require(CLOSEPAREN, "Invalid syntax: ) expected", s);
		return new StatementNode.relopNode(type, arg1, arg2);
	}

	private static StatementNode.opNode parseOp(Scanner s){
		String type = s.next();
		require(OPENPAREN, "Invalid syntax: ( expected", s);
		StatementNode.argumentNode arg1 = parseArgs(s);
		require(COMMA, "Invalid syntax: , expected", s);
		StatementNode.argumentNode arg2 = parseArgs(s);
		require(CLOSEPAREN, "Invalid syntax: ) expected", s);
		return new StatementNode.opNode(type, arg1, arg2);
	}


	/**
	 * Report a failure in the parser.
	 */
	static void fail(String message, Scanner s) {
		String msg = message + "\n   @ ...";
		for (int i = 0; i < 5 && s.hasNext(); i++) {
			msg += " " + s.next();
		}
		throw new ParserFailureException(msg + "...");
	}

	/**
	 * Requires that the next token matches a pattern if it matches, it consumes
	 * and returns the token, if not, it throws an exception with an error
	 * message
	 */
	static String require(String p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	static String require(Pattern p, String message, Scanner s) {
		if (s.hasNext(p)) {
			return s.next();
		}
		fail(message, s);
		return null;
	}

	/**
	 * Requires that the next token matches a pattern (which should only match a
	 * number) if it matches, it consumes and returns the token as an integer if
	 * not, it throws an exception with an error message
	 */
	static int requireInt(String p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	static int requireInt(Pattern p, String message, Scanner s) {
		if (s.hasNext(p) && s.hasNextInt()) {
			return s.nextInt();
		}
		fail(message, s);
		return -1;
	}

	/**
	 * Checks whether the next token in the scanner matches the specified
	 * pattern, if so, consumes the token and return true. Otherwise returns
	 * false without consuming anything.
	 */
	static boolean checkFor(String p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

	static boolean checkFor(Pattern p, Scanner s) {
		if (s.hasNext(p)) {
			s.next();
			return true;
		} else {
			return false;
		}
	}

}

// You could add the node classes here, as long as they are not declared public (or private)
