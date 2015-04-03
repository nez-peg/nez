package nez.expr;

import nez.SourceContext;

public interface Recognizer {
	public boolean match(SourceContext context);
	
//	@Override
//	public boolean match(SourceContext context) {
//		String indent = context.getIndentText(context.getPosition());
//		context.pushSymbolTable(NezTag.Indent, indent);
//		return true;
//	}

//	public boolean matchAnd(SourceContext context) {
//		long pos = context.getPosition();
//		boolean b = this.inner.optimized.match(context);
//		context.rollback(pos);
//		return b;
//	}
//
//	public boolean matchAnyChar(SourceContext context) {
//		if(context.byteAt(context.getPosition()) != context.EOF()) {
//			int len = context.charLength(context.getPosition());
//			context.consume(len);
//			return true;
//		}
//		return context.failure2(this);
//	}
//
//	@Override
//	public boolean matchBlock(SourceContext context) {
//		int stateValue = context.stateValue;
//		String indent = context.getIndentText(context.getPosition());
//		int stackTop = context.pushSymbolTable(NezTag.Indent, indent);
//		boolean b = this.inner.optimized.match(context);
//		context.popSymbolTable(stackTop);
//		context.stateValue = stateValue;
//		return b;
//	}
//	
//	@Override
//	public boolean matchByteChar(SourceContext context) {
//		if(context.byteAt(context.getPosition()) == this.byteChar) {
//			context.consume(1);
//			return true;
//		}
//		return context.failure2(this);
//	}
//	
//	@Override
//	public boolean matchByteMap(SourceContext context) {
//		if(this.byteMap[context.byteAt(context.getPosition())]) {
//			context.consume(1);
//			return true;
//		}
//		return context.failure2(this);
//	}
//	
//	@Override
//	public boolean matchChoice(SourceContext context) {
//		//long f = context.rememberFailure();
//		SyntaxTree left = context.left;
//		for(int i = 0; i < this.size(); i++) {
//			context.left = left;
//			if(this.get(i).optimized.match(context)) {
//				//context.forgetFailure(f);
//				left = null;
//				return true;
//			}
//		}
//		//assert(context.isFailure());
//		left = null;
//		return false;
//	}
//
//	
//	@Override
//	public boolean matchDefSymbol(SourceContext context) {
//		long startIndex = context.getPosition();
//		if(this.inner.optimized.match(context)) {
//			long endIndex = context.getPosition();
//			String s = context.substring(startIndex, endIndex);
//			context.pushSymbolTable(table, s);
//			return true;
//		}
//		return false;
//	}
//
//	@Override
//	public boolean matchIsIndent(SourceContext context) {
//		return context.matchSymbolTable(NezTag.Indent, true);
//	}
//
//	@Override
//	public boolean matchIsSymbol(SourceContext context) {
//		return context.matchSymbolTable(table, this.checkLastSymbolOnly);
//	}
//
//	@Override
//	public boolean matchMatch(SourceContext context) {
//		return this.inner.optimized.match(context);
//	}
//	
//	@Override
//	public boolean matchNonTerminal(SourceContext context) {
//		return context.matchNonTerminal(this);
//	}
//
//	@Override
//	public boolean matchNot(SourceContext context) {
//		long pos = context.getPosition();
//		//long f   = context.rememberFailure();
//		SyntaxTree left = context.left;
//		if(this.inner.optimized.match(context)) {
//			context.rollback(pos);
//			context.failure2(this);
//			left = null;
//			return false;
//		}
//		else {
//			context.rollback(pos);
//			//context.forgetFailure(f);
//			context.left = left;
//			left = null;
//			return true;
//		}
//	}
//
//	@Override
//	public boolean matchOption(SourceContext context) {
//		//long f = context.rememberFailure();
//		SyntaxTree left = context.left;
//		if(!this.inner.optimized.match(context)) {
//			context.left = left;
//			//context.forgetFailure(f);
//		}
//		left = null;
//		return true;
//	}
//
//	@Override
//	public boolean matchRepetition(SourceContext context) {
//		long ppos = -1;
//		long pos = context.getPosition();
////		long f = context.rememberFailure();
//		while(ppos < pos) {
//			SyntaxTree left = context.left;
//			if(!this.inner.optimized.match(context)) {
//				context.left = left;
//				left = null;
//				break;
//			}
//			ppos = pos;
//			pos = context.getPosition();
//			left = null;
//		}
////		context.forgetFailure(f);
//		return true;
//	}
//




}
