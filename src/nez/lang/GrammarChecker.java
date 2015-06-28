package nez.lang;

import java.util.TreeMap;

import nez.NezOption;
import nez.ast.SourcePosition;
import nez.main.Command;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.UMap;

public class GrammarChecker {
	
	boolean strictMode;
	NezOption option;

	boolean foundError = false;
	boolean foundFlag  = false;

//	public GrammarChecker(int checkerLevel) {
//		this.strictMode = checkerLevel > 0;
//	}
	
	public GrammarChecker(int checkerLevel, NezOption option) {
		this.strictMode = checkerLevel > 0;
		this.option = option;
	}
	
	final void foundFlag() {
		this.foundFlag = true;
	}
	
	final void foundFatalError() {
		this.foundError = true;
	}
	
//	public void reportError(SourcePosition s, String message) {
//		if(s != null) {
//			ConsoleUtils.println(s.formatSourceMessage("error", message));
//		}
//	}
//
//	public void reportWarning(SourcePosition s, String message) {
//		if(s != null) {
//			ConsoleUtils.println(s.formatSourceMessage("warning", message));
//		}
//	}
//
//	public void reportNotice(SourcePosition s, String message) {
//		if(this.strictMode) {
//			if(s != null) {
//				ConsoleUtils.println(s.formatSourceMessage("notice", message));
//			}
//		}
//	}

	public void exit(int exit, String message) {
		ConsoleUtils.exit(exit, message);
	}
	
	public final static boolean specialRuleName(String n) {
		return n.equalsIgnoreCase("FILE") || n.equalsIgnoreCase("CHUNK");
	}

}
