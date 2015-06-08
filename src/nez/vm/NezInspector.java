//package nez.vm;
//
//import java.util.HashMap;
//
//import nez.lang.And;
//import nez.lang.AnyChar;
//import nez.lang.Block;
//import nez.lang.ByteChar;
//import nez.lang.ByteMap;
//import nez.lang.Capture;
//import nez.lang.Choice;
//import nez.lang.DefIndent;
//import nez.lang.DefSymbol;
//import nez.lang.ExistsSymbol;
//import nez.lang.Expression;
//import nez.lang.Grammar;
//import nez.lang.IsIndent;
//import nez.lang.IsSymbol;
//import nez.lang.Link;
//import nez.lang.LocalTable;
//import nez.lang.New;
//import nez.lang.NonTerminal;
//import nez.lang.Not;
//import nez.lang.Option;
//import nez.lang.Production;
//import nez.lang.Repetition;
//import nez.lang.Repetition1;
//import nez.lang.Replace;
//import nez.lang.Tagging;
//import nez.util.UCounter;
//import nez.util.UMap;
//
//public class NezInspector extends NezEncoder {
//	
//	public NezInspector(int option) {
//		super(option);
//	}
//
//	UMap<UCounter> statMap = new UMap<UCounter>();
//	HashMap<Integer,Boolean> visitedMap = new HashMap<Integer,Boolean>();
//	
//	private void count(String key) {
//		UCounter u = this.statMap.get(key);
//		if(u == null) {
//			u = new UCounter();
//			this.statMap.put(key, u);
//		}
//		u.count();
//	}
//	
////	@Override
////	public NezCode compile(Grammar grammar) {
////		for(Production p: grammar.getProductionList()) {
////			count("Production");
////			encodeExpression(p.getExpression(), null, null);
////		}
////		return null;
////	}
//
//	@Override
//	public Instruction encodeExpression(Expression e, Instruction next,
//			Instruction failjump) {
//		if(!this.visitedMap.containsKey(e.getId())) {
//			this.visitedMap.put(e.getId(), true);
//			e.encode(this, next, failjump);
//		}
//		return null;
//	}
//
//	@Override
//	public Instruction encodeFail(Expression p) {
//		return null;
//	}
//
//	@Override
//	public Instruction encodeAnyChar(AnyChar p, Instruction next,
//			Instruction failjump) {
//		count("Character");
//		return null;
//	}
//
//	@Override
//	public Instruction encodeByteChar(ByteChar p, Instruction next,
//			Instruction failjump) {
//		count("Character");
//		return null;
//	}
//
//	@Override
//	public Instruction encodeByteMap(ByteMap p, Instruction next,
//			Instruction failjump) {
//		count("ByteMap");
//		return null;
//	}
//
//	@Override
//	public Instruction encodeOption(Option p, Instruction next) {
//		count("Option");
//		return this.encodeExpression(p.get(0), next, null);
//	}
//
//	@Override
//	public Instruction encodeRepetition(Repetition p, Instruction next) {
//		count("Repetition");
//		return this.encodeExpression(p.get(0), next, null);
//	}
//
//	@Override
//	public Instruction encodeRepetition1(Repetition1 p, Instruction next,
//			Instruction failjump) {
//		count("Repetition");
//		return this.encodeExpression(p.get(0), next, failjump);
//	}
//
//	@Override
//	public Instruction encodeAnd(And p, Instruction next, Instruction failjump) {
//		count("And");
//		return this.encodeExpression(p.get(0), next, failjump);
//	}
//
//	@Override
//	public Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
//		count("Not");
//		return this.encodeExpression(p.get(0), next, failjump);
//	}
//
//	@Override
//	public Instruction encodeSequence(Sequence p, Instruction next,
//			Instruction failjump) {
//		this.encodeExpression(p.get(0), next, failjump);
//		return this.encodeExpression(p.get(1), next, failjump);
//	}
//
//	@Override
//	public Instruction encodeChoice(Choice p, Instruction next,
//			Instruction failjump) {
//		count("Choice");
//		return null;
//	}
//
//	@Override
//	public Instruction encodeNonTerminal(NonTerminal p, Instruction next,
//			Instruction failjump) {
//		count("NonTerminal");
//		return null;
//	}
//
//	@Override
//	public Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
//		return this.encodeExpression(p.get(0), next, failjump);
//	}
//
//	@Override
//	public Instruction encodeNew(New p, Instruction next) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeCapture(Capture p, Instruction next) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeTagging(Tagging p, Instruction next) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeReplace(Replace p, Instruction next) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeBlock(Block p, Instruction next,
//			Instruction failjump) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeDefSymbol(DefSymbol p, Instruction next,
//			Instruction failjump) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeIsSymbol(IsSymbol p, Instruction next,
//			Instruction failjump) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeDefIndent(DefIndent p, Instruction next,
//			Instruction failjump) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeIsIndent(IsIndent p, Instruction next,
//			Instruction failjump) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeExistsSymbol(ExistsSymbol existsSymbol,
//			Instruction next, Instruction failjump) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Instruction encodeLocalTable(LocalTable localTable,
//			Instruction next, Instruction failjump) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//}
