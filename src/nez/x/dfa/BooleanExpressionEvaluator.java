package nez.dfa;

public class BooleanExpressionEvaluator {
	private String context;
	private int currentPosition;
	
	public BooleanExpressionEvaluator(String context){
		this.context    = context;
		currentPosition = 0;
	}
	
	boolean fact() {
		boolean not_coef = false;
		if( context.charAt(currentPosition) == '!' ) {
			++currentPosition;
			not_coef = true;
		}
		
		boolean state = false;
		if( context.charAt(currentPosition) == '(' ) {
			++currentPosition;
			state = exp();
			++currentPosition;
		} else {
			state = ( context.charAt(currentPosition++) == 'T' );
		}
		if( not_coef ) {
			state = !state;
		}
		return state;
	}
	
	boolean exp() {
		boolean state1 = fact();
		while( currentPosition < context.length() && ( context.charAt(currentPosition) == '|' || context.charAt(currentPosition) == '&' ) ) {
			char opr = context.charAt(currentPosition++);
			boolean state2 = fact();
			if( opr == '|' ) {
				state1 |= state2;
			} else {
				state1 &= state2;
			}
		}
		return state1;
	}
	
	public boolean eval(){
		return exp();
	}
	
}
