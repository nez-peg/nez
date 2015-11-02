package nez.ast;

public interface Transducer {
	public Object newInstance(Tree<?> node);
}
