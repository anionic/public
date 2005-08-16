/* Generated By:JJTree: Do not edit this line. ASTPrefixOperator.java */

package org.cs3.pl.parser.internal.term;


public class ASTPrefixOperator extends SimpleNode {
	private String value;
	private String label;

	public ASTPrefixOperator(int id) {
		super(id);
	}

	public ASTPrefixOperator(PrologTermParser p, int id) {
		super(p, id);
	}

	public int getPrecedence() {
		return parser.ops.lookupPrefixPrec(getFirstToken().image);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(PrologTermParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}
	public String toString() {
		return super.toString() + " ("+getImage()+")";
	}
	
	protected void synthesizeImage(StringBuffer sb) {
		sb.append("'"+getValue()+"'");

	}

	public String getValue() {
		if(value==null){
			value=getImage();
			if(value.startsWith("'")){
				value=value.substring(1,value.length()-1);
			}
		}
		return value;
	}
	
	public SimpleNode createShallowCopy() {
		ASTPrefixOperator copy = new ASTPrefixOperator(parser,id);
		copy.copy=true;
		copy.value=getValue();
		return copy;
	}
	
	public int getArity() {
		return 0;

	}

	
}
