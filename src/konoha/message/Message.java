package konoha.message;

import java.util.Locale;
import java.util.ResourceBundle;

import nez.main.Verbose;

public enum Message {
	Konoha, Hint, DetectedUTF8, //
	Error, Warning, Notice, Info, //
	SyntaxError, SyntaxError_Expected, //
	TypeError__, UndefinedType_, UndefinedClass_, //
	UndefinedCast__, //
	UndefinedName_, AlreadyDefinedName_As_, ReadOnly, //
	UndefinedField__, NotStaticField__, //
	UndefinedFunctor_, MismatchedFunctor__, //
	Function_, Method__, Unary__, Binary___, Constructor_, //
	Indexer_, //
	UndefinedReturnType_, //
	LeftHandAssignment;
	@Override
	public String toString() {
		try {
			return ResourceBundle.getBundle("konoha.message.Message", Locale.getDefault()).getString(name());
		} catch (java.util.MissingResourceException ex) {
		}
		try {
			return ResourceBundle.getBundle("konoha.message.Message", Locale.ENGLISH).getString(name());
		} catch (java.util.MissingResourceException ex) {
			Verbose.traceException(ex);
			return "[TODO: " + Locale.getDefault() + "] " + name();
		}
	}
}
