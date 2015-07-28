package nez.konoha;

import nez.string.StringTransducer;

public abstract class KonohaTypeRule {
	String    name;
	int       size;
	StringTransducer st;
	KonohaTypeRule nextChoice;

	public KonohaTypeRule(String name, int size, StringTransducer st) {
		this.name = name;
		this.size = size;
		this.st = st;
	}
	
	public final String getName() {
		return this.name;
	}

	public final int size() {
		return this.size;   // -1 many
	}

	public void match(KonohaTransducer konoha, KonohaTree node) {
		node.matched = this;
		node.typed = KonohaType.VoidType;
	}

}



//class ErrorFunctor extends KonohaTypeRule {
//	public ErrorFunctor() {
//		super(BunSymbol.PerrorFunctor, null);
//	}
//
//	@Override
//	protected void matchSubNode(PegObject node) {
//		node.matched = this;
//	}
//
//	@Override
//	public void build(PegObject node, BunDriver driver) {
//		PegObject msgNode = node.get(0, null);
//		if(msgNode != null) {
//			String errorMessage = node.getTextAt(0, "*error*");
//			driver.pushErrorMessage(msgNode.source, errorMessage);
//		}
//		else {
//			driver.pushErrorMessage(node.source, "syntax error");
//		}
//	}
//}
//
//class TypeFunctor extends KonohaTypeRule {
//	public TypeFunctor(String name, BType type) {
//		super(name, BTypePool._LookupFuncType2(type));
//	}
//
//	@Override
//	protected void matchSubNode(PegObject node) {
//		node.matched = this;
//	}
//
//	@Override
//	public void build(PegObject node, BunDriver driver) {
//		driver.pushTypeOf(node);
//	}
//}
//
//class NameFunctor extends KonohaTypeRule {
//	private final int nameIndex;
//	public NameFunctor(String name, int nameIndex, BType type) {
//		super(name, BTypePool._LookupFuncType2(type));
//		this.nameIndex = nameIndex;
//	}
//
//	@Override
//	protected void matchSubNode(PegObject node) {
//		node.matched = this;
//	}
//
//	@Override
//	public void build(PegObject node, BunDriver driver) {
//		driver.pushName(this.name, this.nameIndex);
//	}
//}
//
//class IntegerFunctor extends KonohaTypeRule {
//	public IntegerFunctor() {
//		super("integer", BTypePool._LookupFuncType2(BType.IntType));
//	}
//
//	@Override
//	protected void matchSubNode(PegObject node) {
//		node.matched = this;
//	}
//
//	@Override
//	public void build(PegObject node, BunDriver driver) {
//		driver.pushInteger(node.getText());
//	}
//}
//
//
//class DefineFunctor extends KonohaTypeRule {
//	public DefineFunctor() {
//		super("define", null);
//	}
//
//	@Override
//	protected void matchSubNode(PegObject node) {
//		SymbolTable gamma = node.getSymbolTable();
//		SectionFunctor f = this.newFunctor(gamma, node);
//		if(f != null) {
//			gamma.addFunctor(f);
//		}
//		node.matched = this;
//	}
//
//	@Override
//	public void build(PegObject node, BunDriver driver) {
//		// TODO Auto-generated method stub
//
//	}
//
//	private SectionFunctor newFunctor(SymbolTable gamma, PegObject defineNode) {
//		PegObject sig = defineNode.get(0);
//		String name = sig.getTextAt(0, null);
//		CommonMap<Integer> nameMap = new CommonMap<Integer>(null);
//		BFuncType funcType = this.newFuncType(gamma, sig.get(1), sig.get(2,null), nameMap);
//		SectionFunctor functor = new SectionFunctor(name, funcType);
//		for(int i = 1; i < defineNode.size(); i++) {
//			Section section = this.newSection(defineNode.get(i), nameMap);
//			functor.add(section);
//		}
//		return functor;
//	}
//
//	private BType newType(SymbolTable gamma, PegObject typeNode) {
//		if(typeNode != null) {
//			KonohaTypeRule f = gamma.getFunctor(typeNode);
//			if(f != null) {
//				f.match(typeNode);
//			}
//			System.out.println("debug: newType " + typeNode);
//		}
//		return BType.VoidType;
//	}
//
//	private BFuncType newFuncType(SymbolTable gamma, PegObject paramNode, PegObject returnTypeNode, CommonMap<Integer> nameMap) {
//		CommonArray<BType> typeList = new CommonArray<BType>(new BType[paramNode.size()+1]);
//		for(int i = 0; i < paramNode.size(); i++) {
//			PegObject p = paramNode.get(i);
//			String name = p.getTextAt(0, null);
//			typeList.add(this.newType(gamma, p.get(1, null)));
//			nameMap.put(name, i);
//		}
//		typeList.add(this.newType(gamma, returnTypeNode));
//		return BTypePool._LookupFuncType2(typeList);
//	}
//
//	private Section newSection(PegObject sectionNode, CommonMap<Integer> nameMap) {
//		Section section = new Section();
//		int line = 0;
//		for(int i = 0; i < sectionNode.size(); i++) {
//			PegObject subNode = sectionNode.get(i);
//			//			System.out.println(subNode);
//			if(subNode.is("section.label")) {
//				System.out.println("TODO: section.label");
//			}
//			if(subNode.is("section.line")) {
//				if(line > 0) {
//					section.addNewLine();
//				}
//				section.addLineNode(subNode, nameMap);
//				line = line + 1;
//			}
//		}
//		return section;
//	}
//}
//
//class SectionFunctor extends KonohaTypeRule {
//	Section section;
//
//	public SectionFunctor(String name, BFuncType funcType) {
//		super(name, funcType);
//		this.section = null;
//	}
//
//	@Override
//	protected void matchSubNode(PegObject node) {
//		SymbolTable gamma = node.getSymbolTable();
//		for(int i = 0; i < node.size(); i++) {
//			PegObject subNode = node.get(i);
//			KonohaTypeRule f = gamma.getFunctor(subNode);
//			if(f != null) {
//				if(!f.match(subNode)) {
//					return;
//				}
//			}
//		}
//		node.matched = this;
//	}
//
//	@Override
//	public void build(PegObject node, BunDriver driver) {
//		Section cur = this.section;
//		while(cur != null) {
//			cur.build(node, driver);
//			cur = cur.nextChoice;
//		}
//	}
//
//	public void add(Section section) {
//		Section sec = this.section;
//		if(sec == null) {
//			this.section = section;
//		}
//		else {
//			while(sec.nextChoice != null) {
//				sec = sec.nextChoice;
//			}
//			sec.nextChoice = section;
//		}
//	}
//}
//
//class Section {
//	String label;
//	CommonArray<String> requirements;
//	ChunkCommand chunks = null;
//	Section nextChoice = null;
//
//	public Section() {
//
//	}
//
//	void add(ChunkCommand chunk) {
//		if(this.chunks == null) {
//			this.chunks = chunk;
//		}
//		else {
//			ChunkCommand cur = this.chunks;
//			while(cur.next != null) {
//				cur = cur.next;
//			}
//			cur.next = chunk;
//		}
//	}
//
//	public void build(PegObject node, BunDriver driver) {
//		ChunkCommand cur = this.chunks;
//		//System.out.println("debug command: " + cur);
//		while(cur != null) {
//			cur.push(node, driver);
//			cur = cur.next;
//			//System.out.println("debug command: " + cur);
//		}
//	}
//
//	boolean addLineNode(PegObject lineNode, CommonMap<Integer> nameMap) {
//		for(int j = 0; j < lineNode.size(); j++) {
//			PegObject chunkNode = lineNode.get(j);
//			//System.out.println("debug: chunk: " + chunkNode);
//			if(chunkNode.is("section.chunk")) {
//				String s = chunkNode.getText();
//				if(s.equals("$$")) {
//					s = "$";
//				}
//				this.addChunk(s);
//			}
//			else if(chunkNode.is("section.command")) {
//				if(chunkNode.size() == 1) {
//					String name = chunkNode.getTextAt(0, null);
//					Integer index = nameMap.GetValue(name, null);
//					if(index == null) {
//						System.out.println("undefined name: " + name);
//						return false;
//					}
//					this.addNode(index);
//				}
//				else {
//					String cmd = chunkNode.getTextAt(0, null);
//					String name = chunkNode.getTextAt(1, null);
//					Integer index = nameMap.GetValue(name, null);
//					if(index == null) {
//						System.out.println("undefined name: " + name);
//						return false;
//					}
//					this.addCommand(cmd, index);
//				}
//			}
//		}
//		return true;
//	}
//	void addNewLine() {
//		this.add(new NewLineChunk());
//	}
//	void addChunk(String text) {
//		this.add(new Chunk(text));
//	}
//	void addNode(int index) {
//		this.add(new NodeCommand(null, index));
//	}
//	void addCommand(String cmd, int index) {
//		if(cmd.equals("typeof")) {
//			this.add(new TypeOfNodeCommand(cmd, index));
//			return;
//		}
//		this.add(new Command(cmd, index));
//	}
//}
//
//
//abstract class ChunkCommand {
//	ChunkCommand next = null;
//	public abstract void push(PegObject node, BunDriver d);
//}
//
//class Chunk extends ChunkCommand {
//	String text;
//	Chunk(String text) {
//		this.text = text;
//	}
//	@Override
//	public void push(PegObject node, BunDriver d) {
//		d.pushChunk(this.text);
//	}
//}
//
//class NewLineChunk extends ChunkCommand {
//	@Override
//	public void push(PegObject node, BunDriver d) {
//		d.pushNewLine();
//	}
//}
//
//class Command extends ChunkCommand {
//	String name;
//	int index;
//	Command(String name, int index) {
//		this.name = name;
//		this.index = index;
//	}
//	@Override
//	public void push(PegObject node, BunDriver d) {
//		d.pushCommand(this.name, node.get(this.index));
//	}
//
//
//}
//
//class NodeCommand extends Command {
//	NodeCommand(String name, int index) {
//		super(name, index);
//	}
//	@Override
//	public void push(PegObject node, BunDriver d) {
//		d.pushNode(node.get(this.index));
//	}
//}
//
//class TypeOfNodeCommand extends Command {
//	TypeOfNodeCommand(String name, int index) {
//		super(name, index);
//	}
//	@Override
//	public void push(PegObject node, BunDriver d) {
//		d.pushTypeOf(node.get(this.index));
//	}
//}
//
