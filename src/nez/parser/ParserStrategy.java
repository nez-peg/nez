package nez.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;

import nez.ast.Source;
import nez.ast.SourceLocation;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.parser.moz.MozCompiler;
import nez.parser.moz.MozMachine;
import nez.parser.moz.ParserGrammar;
import nez.util.ConsoleUtils;
import nez.util.Verbose;

public class ParserStrategy {

	/* Grammars */
	public boolean TreeConstruction = true;
	public boolean SymbolTable = true;
	public boolean PEGCompatible = false;

	/* Optimization */
	public boolean Optimization = true;
	public boolean Oinline = true;
	public boolean Oalias = false;

	public boolean Olex = true;
	public boolean Ostring = true;
	public boolean Otrie = true;
	public boolean Odchoice = true;
	public boolean Odfa = false;

	public boolean Oorder = true;

	/* PackratParsing */
	public boolean PackratParsing = true;
	public int SlidingWindow = 32;

	/* Profiling */
	public boolean Profiling = false;
	public boolean Wnone = false;
	public boolean Wall = false;

	public ParserStrategy() {
		init();
	}

	public ParserStrategy(String arguments) {
		init();
		for (String option : arguments.split(" ")) {
			this.setOption(option);
		}
	}

	public final boolean setOption(String option) {
		int loc = option.indexOf('=');
		if (loc > 0) {
			String name = option.substring(0, loc);
			String value = option.substring(loc + 1);
			if (value.equals("true")) {
				return this.setValue(name, true);
			}
			if (value.equals("false")) {
				return this.setValue(name, false);
			}
			try {
				int nvalue = Integer.parseInt(value);
				return setValue(name, nvalue);
			} catch (Exception e) {
			}
			return this.setValue(name, value);
		}
		if (option.startsWith("-")) {
			return this.setValue(option.substring(1), false);
		}
		if (option.startsWith("+")) {
			return this.setValue(option.substring(1), true);
		}
		return false;
	}

	private boolean setValue(String name, Object value) {
		try {
			Field f = this.getClass().getField(name);
			f.set(this, value);
			return true;
		} catch (Exception e) {
			Verbose.traceException(e);
			return false;
		}
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		int c = 0;
		Field[] fields = this.getClass().getFields();
		for (Field f : fields) {
			if (c > 0) {
				sb.append(",");
			}
			if (Modifier.isPublic(f.getModifiers())) {
				sb.append(f.getName());
				sb.append("=");
				try {
					sb.append(f.get(this));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					Verbose.traceException(e);
				}
				c++;
			}
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public final ParserStrategy clone() {
		ParserStrategy s = new ParserStrategy();
		Field[] fields = this.getClass().getFields();
		for (Field f : fields) {
			if (Modifier.isPublic(f.getModifiers())) {
				try {
					f.set(s, f.get(this));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					Verbose.traceException(e);
				}
			}
		}
		return s;
	}

	public final static ParserStrategy nullCheck(ParserStrategy strategy) {
		return strategy == null ? newDefaultStrategy() : strategy;
	}

	public final static ParserStrategy newDefaultStrategy() {
		return new ParserStrategy();
	}

	public final static ParserStrategy newSafeStrategy() {
		return new ParserStrategy("-PackratParsing -Odchoice");
	}

	// ----------------------------------------------------------------------
	// reporter

	private ArrayList<String> logs;
	private HashSet<String> checks;

	void init() {
		if (Wnone) {
			this.logs = new ArrayList<>();
			this.checks = new HashSet<>();
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
			if (!this.Wall) {
				if (s.indexOf("notice") != -1) {
					continue; // skip notice
				}
			}
			ConsoleUtils.println(s);
		}
		this.init();
	}

	public final void reportError(SourceLocation s, String message) {
		if (s != null) {
			log(s.formatSourceMessage("error", message));
		}
	}

	public final void reportWarning(SourceLocation s, String message) {
		if (s != null) {
			log(s.formatSourceMessage("warning", message));
		}
	}

	public final void reportNotice(SourceLocation s, String message) {
		if (s != null) {
			log(s.formatSourceMessage("notice", message));
		}
	}

	public final void reportError(SourceLocation s, String fmt, Object... args) {
		if (s != null) {
			log(s.formatSourceMessage("error", String.format(fmt, args)));
		}
	}

	public final void reportWarning(SourceLocation s, String fmt, Object... args) {
		if (s != null) {
			log(s.formatSourceMessage("warning", String.format(fmt, args)));
		}
	}

	public final void reportNotice(SourceLocation s, String fmt, Object... args) {
		if (s != null) {
			log(s.formatSourceMessage("notice", String.format(fmt, args)));
		}
	}

	/* -------------------------------------------------------------------- */

	public final Parser newParser(Grammar grammar) {
		return grammar.newParser(this);
	}

	public ParserCode<?> newParserCode(ParserGrammar pgrammar) {
		// if (this.isEnabled("Moz", ParserStrategy.Moz)) {
		// MozCompiler mozCompiler = new MozCompiler(this);
		// return mozCompiler.compile(pgrammar);
		// }
		MozCompiler bc = MozCompiler.newCompiler(this);
		return bc.compile(pgrammar);
	}

	public ParserContext newParserContext(Source source, int memoPointSize, Tree<?> prototype) {
		MemoTable table = MemoTable.newTable(this.SlidingWindow, memoPointSize);
		MozMachine machine = new MozMachine(source);
		machine.init(table, prototype);
		return new ParserContext(source, machine);
	}

}
