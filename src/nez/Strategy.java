package nez;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import nez.ast.SourcePosition;
import nez.util.ConsoleUtils;

public class Strategy {

	public final static Strategy nullCheck(Strategy strategy) {
		return strategy == null ? newDefaultStrategy() : strategy;
	}

	public final static Strategy newDefaultStrategy() {
		return new Strategy();
	}

	public final static Strategy newSafeStrategy() {
		return new Strategy("-memo:-Ofirst");
	}

	// grammar
	public final static boolean PEG = false;

	public final static boolean AST = true;
	public final static boolean MEMO = true;
	public final static boolean Mpackrat = false;

	public final static boolean Onone = false;
	public final static boolean Olex = true;
	public final static boolean Ostr = true;
	public final static boolean Otrie = false;

	public final static boolean Oinline = true;
	public final static boolean Oalias = false;
	public final static boolean Ofirst = true;

	public final static boolean Wnone = true;
	public final static boolean Wnotice = true;
	public final static boolean Winfo = false;

	public final static boolean Moz = false; // for MozCompiler

	public static boolean Doption = false; // for verbose option
	public static boolean Dgrammar = false; // for debugging grammar

	public final static boolean PROF = false;

	public static final boolean Odfa = false; // experimental

	public Strategy() {
		init();
	}

	public Strategy(String arguments) {
		this.setOption(arguments);
		init();
	}

	private TreeMap<String, Object> data = new TreeMap<String, Object>();

	public final void setEnabled(String key, boolean flag) {
		data.put(key, flag);
	}

	public final boolean isEnabled(String key, boolean def) {
		Object v = data.get(key);
		if (v instanceof Boolean) {
			return (Boolean) v;
		}
		return def;
	}

	public final boolean isDisabled(String key, boolean def) {
		Object v = data.get(key);
		if (v instanceof Boolean) {
			return !(Boolean) v;
		}
		return !def;
	}

	public final int getInt(String key, int def) {
		Object v = data.get(key);
		if (v instanceof Integer) {
			return (Integer) v;
		}
		return def;
	}

	public final void setValue(String keyvalue) {
		int loc = keyvalue.indexOf('=');
		try {
			if (loc > 0) {
				data.put(keyvalue.substring(0, loc), Integer.parseInt(keyvalue.substring(loc + 1)));
			}
		} catch (NumberFormatException e) {
		}
	}

	public final void setOption(String args) {
		for (String s : args.split(":")) {
			if (s.startsWith("+")) {
				// setOption(s.substring(1), true);
				this.setEnabled(s.substring(1), true);
			} else if (s.startsWith("-")) {
				// setOption(s.substring(1), false);
				this.setEnabled(s.substring(1), false);
			} else {
				// setOption(s, true);
				setValue(s);
			}
		}
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		for (String key : data.keySet()) {
			Object value = data.get(key);
			if (value instanceof Boolean) {
				if ((Boolean) value) {
					sb.append("+" + key);
				} else {
					sb.append("-" + key);
				}
				continue;
			}
			sb.append(key + "=" + value);
		}
		return sb.toString();
	}

	// reporter

	ArrayList<String> logs;
	HashSet<String> checks;

	void init() {
		if (isEnabled("Wnone", Strategy.Wnone)) {
			this.logs = new ArrayList<String>();
			this.checks = new HashSet<String>();
		} else {
			this.logs = null;
			this.checks = null;
		}
	}

	private void log(String msg) {
		if (this.checks != null && !this.checks.contains(msg)) {
			this.checks.add(msg);
			this.logs.add(msg);
		}
	}

	public void report() {
		for (String s : this.logs) {
			if (!this.isEnabled("Wnotice", Strategy.Wnotice)) {
				if (s.indexOf("notice") != -1) {
					continue; // skip notice
				}
			}
			ConsoleUtils.println(s);
		}
		this.init();
	}

	public final void reportError(SourcePosition s, String message) {
		if (s != null) {
			log(s.formatSourceMessage("error", message));
		}
	}

	public final void reportWarning(SourcePosition s, String message) {
		if (s != null) {
			log(s.formatSourceMessage("warning", message));
		}
	}

	public final void reportNotice(SourcePosition s, String message) {
		if (s != null) {
			log(s.formatSourceMessage("notice", message));
		}
	}

	// public boolean classicMode = false; // used "+classic"
	// // public boolean enabledSafeMode = true;
	// // public boolean enabledExperimental = false;
	//
	// /* grammar */
	// public boolean strategyASTConstruction = true; // "-ast"
	// // public boolean enabledSymbolTable = true; // symbol
	// public boolean isEnabled("peg-only", PEG_ONLY") = false; // -peg
	//
	// /* optimization */
	// public boolean enabledAsIsGrammar = false; // asis
	// public boolean isEnabled("Oinline", Strategy.Oinline) = true; // inline
	//
	// public boolean isEnabled("Olex", Strategy.Olex) = true; // "lex"
	// public boolean isEnabled("Ostr", Strategy.Ostr) = true; // "str"
	// // public boolean enabledCommonFactored = true; // common
	//
	// public boolean isEnabled("Ofirst", Strategy.Ofirst) = true; // "predict"
	// public boolean enabledDFAConversion = false; // dfa
	//
	// /* runtime strategy */
	// public boolean isEnabled("memo", Strategy.MEMO) = true; // "memo"
	// public boolean enabledPackratParsing = false; // "packrat"
	//
	// /* misc */
	// public boolean enabledInterning = true;
	// public boolean enabledExampleVerification = false;
	// public boolean isEnabled("prof", Strategy.PROF) = false;
	//
	// public boolean isEnabled("notice", Strategy.NOTICE) = true; // notice
	//
	// public final void setOption(String key, boolean value) {
	// switch (key) {
	// case "asis":
	// this.enabledAsIsGrammar = value;
	// break;
	// case "ast":
	// this.strategyASTConstruction = value;
	// break;
	// case "classic":
	// this.classicMode = value;
	// break;
	// case "dfa":
	// this.enabledDFAConversion = value;
	// break;
	// case "example":
	// this.enabledExampleVerification = value;
	// break;
	// case "inline":
	// this.isEnabled("Oinline", Strategy.Oinline) = value;
	// break;
	// case "intern":
	// this.enabledInterning = value;
	// break;
	// case "lex":
	// case "lexer":
	// this.isEnabled("Olex", Strategy.Olex) = value;
	// break;
	// case "memo":
	// this.isEnabled("memo", Strategy.MEMO) = value;
	// break;
	// case "notice":
	// this.isEnabled("notice", Strategy.NOTICE) = value;
	// break;
	// case "packrat":
	// this.enabledPackratParsing = value;
	// break;
	// case "peg":
	// this.isEnabled("peg-only", PEG_ONLY") = value;
	// break;
	// case "predict":
	// this.isEnabled("Ofirst", Strategy.Ofirst) = value;
	// break;
	// case "prof":
	// this.isEnabled("prof", Strategy.PROF) = value;
	// break;
	// case "str":
	// this.isEnabled("Ostr", Strategy.Ostr) = value;
	// break;
	// default:
	// Verbose.debug("undefined strategy:" + key + " " + value);
	// }
	// }

}
