package nez.ast.jcode;

import nez.ast.Tag;

public interface JCodeTree<T> {
	public Class<?> getTypedClass();

	public T dup();

	public Tag getTag();
}
