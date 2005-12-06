/* Generated By:JJTree: Do not edit this line. ASTCharacters.java */

package org.cs3.pl.parser.internal.term;

import org.cs3.pl.common.Util;

public class ASTCharacters extends SimpleNode implements Atomic{
	
	

	public ASTCharacters(int id) {
		super(id);
	}

	public ASTCharacters(PrologTermParser p, int id) {
		super(p, id);
	}

	/** Accept the visitor. * */
	public Object jjtAccept(PrologTermParserVisitor visitor, Object data) {
		return visitor.visit(this, data);
	}

	public String toString() {
		return super.toString() + " (" + getImage() + ")";
	}

	protected void synthesizeImage(StringBuffer sb) {
		
		sb.append("'"+getValue()+"'");
		

	}

	public String getValue() {
		if (value == null) {
			if(copy){
				throw new IllegalStateException("copy with uninitialized value");
			}
			value = Util.unquoteAtom(getImage());			
			value = value.substring(1, value.length() - 1);
			
		}
		return value;
	}

	public SimpleNode createShallowCopy() {
		ASTCharacters copy = new ASTCharacters(parser, id);
		copy.copy = true;
		copy.value = getValue();
		return copy;
	}
	
	
	
	public SimpleNode toCanonicalTerm(boolean linked, boolean deep) {
//		if(TermParserUtils.shouldBeQuoted(getValue())){
//			return super.toCanonicalTerm(linked, deep);
//		}
		ASTIdentifier copy = new ASTIdentifier(parser,PrologTermParser.IDENTIFIER);
		copy.copy=true;
		if(linked){
			copy.original=this;
		}
		copy.value=getValue();
		return copy;
	}
}
