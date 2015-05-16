package nez.lang;

import java.util.HashMap;
import java.util.List;

import nez.ast.CommonTree;
import nez.util.StringUtils;

public abstract class Formatter {
	public static final Formatter Null = new NullFormatter();
	public static final Formatter Default = new DefaultFormatter();
	
	public static final HashMap<String, Formatter> fmtMap = new HashMap<String,Formatter>();
	static {
		fmtMap.put("NL", new IndentFormatter());
		fmtMap.put("inc", new IncFormatter());
		fmtMap.put("dec", new DecFormatter());
		fmtMap.put("text", Null);
	}
	
	public final static Formatter newAction(String t) {
		return fmtMap.get(t);
	}

	public final static Formatter newFormatter(String t) {
		return new TextFormatter(t);
	}

	public final static Formatter newFormatter(int index) {
		return new IndexFormatter(index);
	}

	public final static Formatter newFormatter(int start, Formatter delim, int end) {
		return new RangeFormatter(start, delim, end);
	}

	public final static Formatter newFormatter(List<Formatter> s) {
		if(s.size() == 1) {
			return s.get(0);
		}
		return new SequenceFormatter(s);
	}

	public static int index(int index, int size) {
		return (index < 0) ? size + index + 1: index;
	}
	
	public static boolean isSupported(NameSpace ns, CommonTree node) {
		Formatter fmt = ns.getFormatter(node.getTag().getName(), node.size());
		return fmt != null;
	}

	public static String format(NameSpace ns, CommonTree node) {
		FormatStringBuilder fsb = new FormatStringBuilder(ns);
		format(fsb, node);
		return fsb.toString();
	}

	static void format(FormatterStream stream, CommonTree node) {
		if(node != null) {
			Formatter fmt = stream.lookupFormatter(node);
			fmt.write(stream, node);
		}
		else {
			stream.write("null");
		}
	}

	
	public abstract void write(FormatterStream stream, CommonTree node);


}

class FormatterEntry {
	Formatter[] arguments;
	void set(int index, Formatter fmt) {
		index++;
		if(arguments == null) {
			arguments = new Formatter[index+1];
			arguments[index] = fmt;
			return;
		}
		if(!(index < arguments.length)) {
			Formatter[] a = new Formatter[index+1];
			System.arraycopy(this.arguments, 0, a, 0, this.arguments.length);
			this.arguments = a;
		}
		arguments[index] = fmt;
	}
	Formatter get(int index) {
		if(arguments == null) {
			return null;
		}
		index++;
		if(!(index < arguments.length)) {
			index = arguments.length - 1;
		}
		for(int i = index; i >=0; i--) {
			if(this.arguments[i] != null) {
				return this.arguments[i];
			}
		}
		return null;
	}
}

class FormatterMap {
	HashMap<String, FormatterEntry> map = new HashMap<String, FormatterEntry>();
	void set(String tag, int index, Formatter fmt) {
		FormatterEntry entry = map.get(tag);
		if(entry == null) {
			entry = new FormatterEntry();
			map.put(tag, entry);
		}
		entry.set(index, fmt);
	}
	Formatter get(String tag, int index) {
		FormatterEntry entry = map.get(tag);
		if(entry != null) {
			return entry.get(index);
		}
		return null;
	}
}


interface FormatterStream {

	public Formatter lookupFormatter(CommonTree sub);
	public void write(String text);
	public void writeNewLineIndent();
	public void incIndent();
	public void decIndent();
	
}

class FormatStringBuilder implements FormatterStream {
	final NameSpace ns;
	StringBuilder sb = new StringBuilder();
	int indent = 0;
	FormatStringBuilder(NameSpace ns) {
		this.ns = ns;
	}
	
	public String toString() {
		return sb.toString();
	}

	@Override
	public Formatter lookupFormatter(CommonTree sub) {
		Formatter fmt = ns.getFormatter(sub.getTag().getName(), sub.size());
		if(fmt == null) {
			return Formatter.Default;
		}
		return fmt;
	}

	@Override
	public void write(String text) {
		sb.append(text);
	}

	@Override
	public void writeNewLineIndent() {
		sb.append("\n");
		for(int i = 0; i < indent; i++) {
			sb.append("   ");
		}
	}

	@Override
	public void incIndent() {
		indent++;
	}

	@Override
	public void decIndent() {
		indent--;
	}
}

class DefaultFormatter extends Formatter {
	DefaultFormatter() {
	}
	@Override
	public void write(FormatterStream s, CommonTree node) {
		s.write("#");
		s.write(node.getTag().getName());
		s.write("[");
		if(node.size() == 0) {
			s.write(StringUtils.quoteString('\'', node.getText(), '\''));
		}
		else {
			int c = 0;
			for(CommonTree sub: node) {
				if(c > 0) {
					s.write(" ");
				}
				Formatter.format(s, sub);
				c++;
			}
		}
		s.write("]");
	}
}

class SequenceFormatter extends Formatter {
	final Formatter sub[];
	SequenceFormatter(List<Formatter> s) {
		sub = new Formatter[s.size()];
		for(int i = 0; i < s.size(); i++) {
			sub[i] = s.get(i);
		}
	}
	@Override
	public void write(FormatterStream s, CommonTree node) {
		for(Formatter fmt: sub) {
			fmt.write(s, node);
		}
	}
}

class NullFormatter extends Formatter {
	NullFormatter() {
	}
	@Override
	public void write(FormatterStream s, CommonTree node) {
		s.write(node.getText());
	}
}

class TextFormatter extends Formatter {
	final String text;
	TextFormatter(String text) {
		this.text = text;
	}
	@Override
	public void write(FormatterStream s, CommonTree node) {
		s.write(text);
	}
}

class IndexFormatter extends Formatter {
	final int index;
	IndexFormatter(int index) {
		this.index = index;
	}
	@Override
	public void write(FormatterStream s, CommonTree node) {
		int size = node.size();
		int index = Formatter.index(this.index, size);
		if(0 <= index && index < size) {
			CommonTree sub = node.get(index);
			Formatter.format(s, sub);
		}
	}
}

class RangeFormatter extends Formatter {
	int start;
	Formatter delim;
	int end;
	RangeFormatter(int s, Formatter delim, int e) {
		this.start = s;
		this.delim = delim;
		this.end = e;
	}
	@Override
	public void write(FormatterStream stream, CommonTree node) {
		int size = node.size();
		int s = Formatter.index(this.start, size);
		int e = Formatter.index(this.end, size);
		if(e > size) {
			e = size;
		}
		for(int i = s; i < e; i++) {
			if(i > s) {
				delim.write(stream, node);
			}
			CommonTree sub = node.get(i);
			Formatter.format(stream, sub);
		}
	}
}

class IndentFormatter extends Formatter {
	IndentFormatter() {
	}
	@Override
	public void write(FormatterStream stream, CommonTree node) {
		stream.writeNewLineIndent();
	}
}

class IncFormatter extends Formatter {
	@Override
	public void write(FormatterStream stream, CommonTree node) {
		stream.incIndent();
	}
}

class DecFormatter extends Formatter {
	@Override
	public void write(FormatterStream stream, CommonTree node) {
		stream.decIndent();
	}
}