package nez.generator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.IfFlag;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.OnFlag;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.main.Verbose;
import nez.util.FileBuilder;
import nez.util.UFlag;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public abstract class NezGenerator extends NezEncoder {
	public abstract String getDesc();

	public NezGenerator() {
		super(0);
		this.file = null;
	}

	protected void setOption(int option) {
		this.option = option;
	}

	protected FileBuilder file;

	protected void setOutputFile(String fileName) {
		if(fileName == null) {
			this.file = new FileBuilder();
		}
		else {
			this.file = new FileBuilder(fileName);
		}
	}
	
//	HashMap<Class<?>, Method> methodMap = new HashMap<Class<?>, Method>();
//
//	public final void visitExpression(Expression p) {
//		Method m = lookupMethod("visit", p.getClass());
//		if(m != null) {
//			try {
//				m.invoke(this, p);
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				e.printStackTrace();
//			}
//		}
//		else {
//			visitUndefined(p);
//		}
//	}
//
//	void visitUndefined(Expression p) {
//		Verbose.todo("undefined: " + p.getClass());
//	}
//
//	protected final Method lookupMethod(String method, Class<?> c) {
//		Method m = this.methodMap.get(c);
//		if(m == null) {
//			String name = method + c.getSimpleName();
//			try {
//				m = this.getClass().getMethod(name, c);
//			} catch (NoSuchMethodException e) {
//				return null;
//			} catch (SecurityException e) {
//				return null;
//			}
//			this.methodMap.put(c, m);
//		}
//		return m;
//	}

	public Instruction encodeExpression(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	public Instruction encodeAnyChar(AnyChar p, Instruction next, Instruction failjump) {
		this.visitAnyChar(p);
		return null;
	}

	public Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump) {
		this.visitByteChar(p);
		return null;
	}

	public Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump) {
		this.visitByteMap(p);
		return null;
	}

	public Instruction encodeFail(Expression p) {
		this.visitFailure(p);
		return null;
	}

	public Instruction encodeOption(Option p, Instruction next) {
		this.visitOption(p);
		return null;
	}

	public Instruction encodeRepetition(Repetition p, Instruction next) {
		this.visitRepetition(p);
		return null;
	}

	public Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump) {
		this.visitRepetition1(p);
		return null;
	}

	public Instruction encodeAnd(And p, Instruction next, Instruction failjump) {
		this.visitAnd(p);
		return null;
	}

	public Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		this.visitNot(p);
		return null;
	}

	public Instruction encodeSequence(Sequence p, Instruction next, Instruction failjump) {
		this.visitSequence(p);
		return null;
	}

	public Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		this.visitChoice(p);
		return null;
	}

	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		this.visitNonTerminal(p);
		return null;
	}

	// AST Construction

	public Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if(is(Grammar.ASTConstruction)) {
			this.visitLink(p);
		}
		else {
			this.visitExpression(p.get(0));
		}
		return null;
	}

	public Instruction encodeNew(New p, Instruction next) {
		if(is(Grammar.ASTConstruction)) {
			this.visitNew(p);
		}
		return null;
	}

	public Instruction encodeCapture(Capture p, Instruction next) {
		if(is(Grammar.ASTConstruction)) {
			this.visitCapture(p);
		}
		return null;
	}

	public Instruction encodeTagging(Tagging p, Instruction next) {
		if(is(Grammar.ASTConstruction)) {
			this.visitTagging(p);
		}
		return null;
	}

	public Instruction encodeReplace(Replace p, Instruction next) {
		if(is(Grammar.ASTConstruction)) {
			this.visitReplace(p);
		}
		return null;
	}

	public Instruction encodeBlock(Block p, Instruction next, Instruction failjump) {
		this.visitBlock(p);
		return null;
	}

	public Instruction encodeLocalTable(LocalTable p, Instruction next, Instruction failjump) {
		this.visitLocalTable(p);
		return null;
	}

	public Instruction encodeDefSymbol(DefSymbol p, Instruction next, Instruction failjump) {
		this.visitDefSymbol(p);
		return null;
	}

	public Instruction encodeExistsSymbol(ExistsSymbol p, Instruction next, Instruction failjump) {
		this.visitExistsSymbol(p);
		return null;
	}

	public Instruction encodeIsSymbol(IsSymbol p, Instruction next, Instruction failjump) {
		this.visitIsSymbol(p);
		return null;
	}

	public Instruction encodeDefIndent(DefIndent p, Instruction next, Instruction failjump) {
		this.visitDefIndent(p);
		return null;
	}

	public Instruction encodeIsIndent(IsIndent p, Instruction next, Instruction failjump) {
		this.visitIsIndent(p);
		return null;
	}
	
	public Instruction encodeEmpty(Expression p, Instruction next) {
		this.visitEmpty(p);
		return null;
	}

	public Instruction encodeOnFlag(OnFlag p, Instruction next, Instruction failjump) {
		return p.get(0).encode(this, next, failjump);
	}

	public Instruction encodeIfFlag(IfFlag ifFlag, Instruction next, Instruction failjump) {
		return next;
	}
	
	@Override
	public Instruction encodeExtension(Expression p, Instruction next, Instruction failjump) {
		this.visitUndefined(p);
		return null;
	}

	public void visitExpression(Expression e) {
		e.encode(this, null, null);
	}

	public void visitUndefined(Expression p) {
		
	}

	public abstract void visitEmpty(Expression p);
	public abstract void visitFailure(Expression p);
	public abstract void visitAnyChar(AnyChar p);
	public abstract void visitByteChar(ByteChar p);
	public abstract void visitByteMap(ByteMap p);
	public abstract void visitOption(Option p);
	public abstract void visitRepetition(Repetition p);
	public abstract void visitRepetition1(Repetition1 p);
	public abstract void visitAnd(And p);
	public abstract void visitNot(Not p);
	public abstract void visitSequence(Sequence p);
	public abstract void visitChoice(Choice p);
	public abstract void visitNonTerminal(NonTerminal p);
	
	// AST Construction
	public abstract void visitLink(Link p);
	public abstract void visitNew(New p);
	public abstract void visitCapture(Capture p);
	public abstract void visitTagging(Tagging p);
	public abstract void visitReplace(Replace p);
	
	// Symbol Tables
	public abstract void visitBlock(Block p);
	public abstract void visitDefSymbol(DefSymbol p);
	public abstract void visitIsSymbol(IsSymbol p);	
	public abstract void visitDefIndent(DefIndent p);
	public abstract void visitIsIndent(IsIndent p);
	public abstract void visitExistsSymbol(ExistsSymbol p);
	public abstract void visitLocalTable(LocalTable p);

	// ---------------------------------------------------------------------

	public void generate(Grammar grammar, int option, String fileName) {
		this.setOption(option);
		this.setOutputFile(fileName);
		makeHeader(grammar);
		for(Production p : grammar.getProductionList()) {
			visitProduction(p);
		}
		makeFooter(grammar);
		file.writeNewLine();
		file.flush();
	}

	public void makeHeader(Grammar g) {
	}

	public abstract void visitProduction(Production r);

	public void makeFooter(Grammar g) {
	}
}
