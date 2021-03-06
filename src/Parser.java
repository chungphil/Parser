import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.*;
import javax.swing.JFileChooser;

/**
 * The parser and interpreter. The top level parse function, a main method for
 * testing, and several utility methods are provided. You need to implement
 * parseProgram and all the rest of the parser.
 */
public class Parser {

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
			// prints out program in raw text form
//			while(scan.hasNext()){
//				System.out.println(scan.next());
//			}
			boolean test = Pattern.matches("(a|b|c)*d(e+)f","abbcccdef");
			if(test){System.out.println("same");}

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

	static Pattern NUMPAT = Pattern.compile("-?\\d+"); // ("-?(0|[1-9][0-9]*)");
	static Pattern OPENPAREN = Pattern.compile("\\(");
	static Pattern CLOSEPAREN = Pattern.compile("\\)");
	static Pattern OPENBRACE = Pattern.compile("\\{");
	static Pattern CLOSEBRACE = Pattern.compile("\\}");
	static Pattern actPat = Pattern.compile("move|turnL|turnR|turnAround|takeFuel|wait|shieldOn|shieldOff");
	static Pattern semiCol = Pattern.compile(";");
	static Pattern loop = Pattern.compile("loop");
	static Pattern SENSOR = Pattern.compile("fuelLeft|oppLR|oppFB|numBarrels|barrelLR|barrelFB|wallDist");
	static Pattern relOP = Pattern.compile("lt|gt|eq");
	static Pattern OP = Pattern.compile("add|sub|mul|div");
	static Pattern AON = Pattern.compile("and|or|not");


	/**
	 * See assignment handout for the grammar.
	 */
	static RobotProgramNode parseProgram(Scanner s) {
		// THE PARSER GOES HERE
		//Can have 0 to ~ number of statements
		ArrayList<RobotProgramNode> sumProgram = new ArrayList<>();
		if(!s.hasNext()){return null;}
		while(s.hasNext()){
			RobotProgramNode statement = parseStmt(s);
			sumProgram.add(statement);
		}

		return new progNode(sumProgram);
	}

	static RobotProgramNode parseStmt(Scanner s) {
		RobotProgramNode returnNode = null;
		//Can be either and Action; or Loop{}
		if(!s.hasNext()){return null;}
		if(s.hasNext(actPat)) { returnNode = parseAct(s); }
		else if (s.hasNext(loop)){ returnNode = parseLoop(s); }
		else if (s.hasNext("if")){ returnNode = parseIf(s);}
		else if (s.hasNext("while")){ returnNode = parseWhile(s);}
		else {fail("Invalid statement", s);}

		return returnNode;
	}

	static RobotProgramNode parseAct( Scanner s) {
		RobotProgramNode returnNode = null;
		// Need to figure out how to distinguish between different acts and parse as correct node.
		if(s.hasNext("move")){
			returnNode = new moveNode(s,null);
			if(s.hasNext("\\(")){
				s.next(); //open par
				expNode EN = parseExp(s);
				if(s.hasNext("\\)")){
					returnNode = new moveNode(s, EN);
				}else{fail("Missing ')'",s);}
			}
		}
		else if(s.hasNext("wait")){
			returnNode = new waitNode(s,null);
			if(s.hasNext("\\(")){
				s.next(); //eat open par
				expNode EN = parseExp(s);
				if(s.hasNext("\\)")){
					returnNode = new waitNode(s, EN);
				}else{fail("Missing ')'",s);}
			}
		}
		else if(s.hasNext("turnL")){returnNode = new turnLnode(s);}
		else if(s.hasNext("turnR")){returnNode = new turnRnode(s);}
		else if(s.hasNext("takeFuel")){returnNode = new takeFnode(s);}
		else if(s.hasNext("turnAround")){returnNode = new turnAnode(s);}
		else if(s.hasNext("shieldOn")){returnNode = new shieldONnode(s);}
		else if(s.hasNext("shieldOff")){returnNode = new shieldOFFnode(s);}
		else {fail("Unknown or missing Act",s);}

		require(semiCol,"Missing ;",s);
		return returnNode;
	}

	static RobotProgramNode parseLoop(Scanner s) {
		RobotProgramNode returnNode = null;
		RobotProgramNode blockN = null;
		require(loop,"Loop Missing",s);
		//This is parsing a block Node
		if(s.hasNext(OPENBRACE)){blockN = parseBlock(s);}
		else {fail("No statements in the loop",s);}
		returnNode = new loopNode(blockN);

		return returnNode;
	}

	static RobotProgramNode parseBlock(Scanner s) {
		ArrayList<RobotProgramNode> blockList = new ArrayList<>();
		require(OPENBRACE,"Missing '{'",s);
		while(!s.hasNext(CLOSEBRACE) && s.hasNext()){
			RobotProgramNode statement = parseStmt(s);
			blockList.add(statement);
		}
		require(CLOSEBRACE, "Missing '}'", s);
		if(!(blockList.size() > 0)){fail("Block is empty",s);}
		return new blockNode(blockList);
	}

	//parse if loop

	static RobotProgramNode parseIf(Scanner s) {
		RobotProgramNode returnNode = null;
		condNode condN = null;
		RobotProgramNode blockN = null;
		RobotProgramNode eBlock = null;

		require("if","'If' Missing",s);
		require(OPENPAREN,"Missing '('",s);
		condN = parseCond(s);
		require(CLOSEPAREN,"Missing ')'",s);
		blockN = parseBlock(s);
		if(s.hasNext("else")){
			s.next(); //eat "else"
			eBlock = parseBlock(s);
		}

		returnNode = new ifNode(condN,blockN,eBlock);

		return returnNode;
	}
	//parse while loop

	static RobotProgramNode parseWhile(Scanner s) {
		RobotProgramNode returnNode = null;
		condNode condN = null;
		RobotProgramNode blockN = null;

		require("while","'While' Missing",s);
		require(OPENPAREN,"Missing '('",s);
		condN = parseCond(s);
		require(CLOSEPAREN,"Missing '('",s);
		blockN = parseBlock(s);

		returnNode = new whileNode(condN,blockN);

		return returnNode;
	}

	static expNode parseExp(Scanner s){
		expNode returnNode = null;
		// Need to figure out how to distinguish between different acts and parse as correct node.
		if(s.hasNext(SENSOR)){
			SensorNode senExp = parseSen(s);
			returnNode = new expNode(senExp);
		}
		else if(s.hasNextInt()){returnNode = new expNode(s.nextInt());}
		else if(s.hasNext(OP)){
			String strOP = s.next();
			require(OPENPAREN,"Missing '('",s);// (
			expNode exp1 = parseExp(s);
			require(",", "Missing ','", s);// ,
			expNode exp2 = parseExp(s);
			require(CLOSEPAREN, "Missing ')'", s);// )
			returnNode = new expNode(strOP,exp1,exp2);
		}
		else {fail("Unknown or missing Sensor term",s);}

		return returnNode;
	}
	//parse
	static condNode parseCond(Scanner s) {
		String relopN = null;
		condNode condReturn = null;

		if(s.hasNext(relOP)){
			relopN = s.next();
			require(OPENPAREN,"Missing '('",s);// (
			expNode exp1 = parseExp(s);
			require(",", "Missing ','", s);// ,
			expNode exp2 = parseExp(s);
			require(CLOSEPAREN, "Missing ')'", s);// )
			condReturn = new condNode(relopN,exp1,exp2);
		}
		else if(s.hasNext("and|or")){
			relopN = s.next();
			require(OPENPAREN,"Missing '('",s);// (
			condNode cn1 = parseCond(s);
			require(",", "Missing ','", s);// ,
			condNode cn2 = parseCond(s);
			require(CLOSEPAREN, "Missing ')'", s);// )
			condReturn = new condNode(relopN,cn1,cn2);
		}
		else if(s.hasNext("not")){
			relopN = s.next();
			require(OPENPAREN,"Missing '('",s);// (
			condNode cn1 = parseCond(s);
			require(CLOSEPAREN, "Missing ')'", s);// )
			condReturn = new condNode(relopN,cn1,cn1);
		}else{fail("Operating Missing",s);}

		return condReturn;
	}
	//parse Sensor
	static SensorNode parseSen( Scanner s) {
		SensorNode returnNode = null;
		// Need to figure out how to distinguish between different acts and parse as correct node.
		if(s.hasNext("fuelLeft")){returnNode = new fuelLnode(s);}
		else if(s.hasNext("oppLR")){returnNode = new oppLRnode(s);}
		else if(s.hasNext("oppFB")){returnNode = new oppFBnode(s);}
		else if(s.hasNext("numBarrels")){returnNode = new numBnode(s);}
		else if(s.hasNext("barrelLR")){returnNode = new barLRnode(s);}
		else if(s.hasNext("barrelFB")){returnNode = new barFBnode(s);}
		else if(s.hasNext("wallDist")){returnNode = new wallDnode(s);}

		else {fail("Unknown or missing Sensor term",s);}

		return returnNode;
	}

	// utility methods for the parser

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


