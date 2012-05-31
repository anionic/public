package org.cs3.pdt.internal.contentassistant;

import java.util.Map;

import org.cs3.pdt.PDT;
import org.cs3.pdt.PDTPlugin;
import org.cs3.pdt.internal.ImageRepository;
import org.cs3.pl.common.logging.Debug;
import org.cs3.pl.cterm.CTerm;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

public class PredicateCompletionProposal extends ComparableCompletionProposal implements ICompletionProposalExtension5,ICompletionProposalExtension3, ICompletionProposalExtension2, IInformationControlCreator{
	private static final String SPAN_HIDDEN = "<span style={display:none}>";
	private int offset;
	private int length;
	private int arity;
	private String name;	
	private Map<String,?> tags;
	private String label;
//	private String module;
	private String doc;
	private String insertion;
	private boolean isModule = false;
	private boolean isAtom = false;
	private TemplateProposal target2;
	private boolean createArglist;
	
	public boolean isModule() {
	return isModule;
}

public boolean isAtom() {
	return isAtom;
}

public PredicateCompletionProposal(IDocument document, int offset, int length,
		String name, int arity, Map<String,?> tags, String module) {
	this(document, offset, length, name, arity, tags, module, null);
}

	/**
	 * 
	 * @param offset
	 * @param length
	 * @param name
	 * @param arity -2: atom, -1 : module, >= 0 : predicate
	 * @param tags
	 * @param module
	 */
	public PredicateCompletionProposal(IDocument document, int offset, int length,
			String name, int arity, Map<String,?> tags, String module, String kind) {
		super(name,offset,length,name.length(),
				ImageRepository.getImage(
						"module".equals(kind)?
								ImageRepository.PACKAGE :
						"atom".equals(kind)?
								ImageRepository.PE_ATOM :
						isBuiltIn(tags)?
								ImageRepository.PE_BUILT_IN :
						isPublic(tags)?
								ImageRepository.PE_PUBLIC:ImageRepository.PE_PROTECTED),
						null,null,null);
		this.offset = offset;
		this.length = length;
		this.name=name;
		this.tags=tags;
		this.arity=arity;
//		this.module=module;
		if(kind != null && !"predicate".equals(kind)){
			if("module".equals(kind)){
				isModule = true;
			} else	if("atom".equals(kind)){
				isAtom = true;
			}
			this.label=name;
		} else {
			this.label = name+"/"+arity;

			CTerm summary = (CTerm) tags.get("summary");
			// FIXME: this only works for exported predicates 
			//    for not exported predicates: predicate_manual_entry(Module,PredName,Arity,Help) will set Help to nodoc
			Object doc = tags.get("documentation");

			if(doc != null){
				String value;
				if(doc instanceof CTerm){
					value =((CTerm)doc).getFunctorValue();
				} else {
					value = ((String)doc).trim();
				}
				this.doc = value;
			} else if(summary!=null){
				label = label + " - " + summary.getFunctorValue();
			}
		}
		insertion = createInsertion();
		if(module!=null && !isAtom){
			label=module+":" + label;
		}
		
		createArglist = Boolean.parseBoolean(PDTPlugin.getDefault().getPreferenceValue(PDT.PREF_AUTO_COMPLETE_ARGLIST, "true"));
		
		final String contextTypeId = "MyContextType";
		Template template = new Template(insertion, "InsertMe", contextTypeId , insertion, true);
		TemplateContextType type = new TemplateContextType(contextTypeId);
		DocumentTemplateContext cxt = new DocumentTemplateContext(type, document , offset, length);
		target2 = new TemplateProposal(template, cxt, new Region(offset, length), image);
	}

	private static boolean isPublic(Map<String, ?> tags) {
		return tags.containsKey("public");
	}
	
	private static boolean isBuiltIn(Map<String, ?> tags) {
		return tags.containsKey("built_in");
	}

	public boolean isPublic() {
		return isPublic(tags);
	}

	public boolean isBuiltIn() {
		return isBuiltIn(tags);
	}

	@Override
	public void apply(IDocument document) {
		try {
			if (createArglist) {
				if(insertion != null) {
					document.replace(offset, length, insertion);
				} else if (arity > 0) {
					StringBuffer buf = new StringBuffer(name);
					buf.append("(");
					for (int i=0; i<arity; i++) {
						if (i==0) {
							buf.append("_");
						} else {
							buf.append(",_");
						}
					}
					buf.append(")");
					document.replace(offset,length,buf.toString());
				} else {
					document.replace(offset, length, name);
				}
			} else {
				document.replace(offset, length, name);
			}
			
		} catch (BadLocationException e) {
			Debug.report(e);
		}
	}
	
	private String createInsertion() {
		if (arity <= 0) {
			return name;
		}
		String[] args = null; 
		if (doc != null) {
			if(doc.startsWith("<dl>\n<dt")){
				if (doc.indexOf("arglist") > 0 && doc.indexOf("</var>") > doc.indexOf("arglist")) {
					String commaSeparatedArgs = doc.substring(doc.indexOf("arglist") + 10, doc.indexOf("</var>") -1);
					commaSeparatedArgs = commaSeparatedArgs.replaceAll("\\?", "");
					commaSeparatedArgs = commaSeparatedArgs.replaceAll("\\-", "");
					commaSeparatedArgs = commaSeparatedArgs.replaceAll("\\+", "");
					args = commaSeparatedArgs.split(",");
					for (int i = 0; i < args.length; i++) {
						int typeSeparator = args[i].indexOf(':');
						if (typeSeparator >= 0) {
							args[i] = args[i].substring(0, typeSeparator);
						}
					}
				}
			} else if (doc.indexOf(SPAN_HIDDEN) > 0 && doc.indexOf("</span>") > 0) {
				String head = doc.substring(doc.indexOf(SPAN_HIDDEN) + SPAN_HIDDEN.length(), doc.indexOf("</span>"));
				args = head.substring(head.indexOf("(") + 1, head.lastIndexOf(")")).split(",");
			}
		}
		StringBuffer buf = new StringBuffer(name);
		buf.append("(");
		if (args == null) {
			for (int i=0; i<arity; i++) {
				if (i==0) {
					buf.append("${_");
				} else {
					buf.append(", ${_");
				}
				buf.append(i);
				buf.append("}");
			}
		} else {
			for (int i=0; i<arity; i++) {
				if (i==0) {
					buf.append("${");
					buf.append(args[i].trim());
					buf.append("}");
				} else {
					buf.append(", ${");
					buf.append(args[i].trim());
					buf.append("}");
				}
			}
		}
		buf.append(")");
		return buf.toString();
	}

	/**
	 * TRHO: Ugly, summery is generated by pldoc entries, documentation comes for built-ins 
	 */
	@Override
	public String getDisplayString() {
		return getLabel();
	}

	private String getLabel() {
		return label;
	}

	@Override
	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return completionOffset;
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document,
			int completionOffset) {

		return getDisplayString();
	}

	@Override
	public IInformationControl createInformationControl(Shell parent) {
		return new BrowserInformationControl(parent);
	}
	
	/**
	 * Format needs improvement.
	 */
	@Override
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		if(doc!=null) {
			if(doc.indexOf("\n")>-1){
				doc="<b>"+doc.replaceFirst("\n", "</b><br/>").replace("\n", "<br/>");
			}
			return doc;
		}
		CTerm summary = (CTerm) tags.get("summary");
		if(summary!=null){
			return "<b>"+getLabel()+"</b></p>"+summary.getFunctorValue();
		}
		return null;
	}

	@Override
	public IInformationControlCreator getInformationControlCreator() {
		return this;
	}


	@Override
	public int compareTo(ComparableCompletionProposal o) {
		if (o instanceof PredicateCompletionProposal){
			PredicateCompletionProposal other = (PredicateCompletionProposal)o;
			if(isBuiltIn() && !other.isBuiltIn()) {
				return 1;
			} if(!isBuiltIn() && other.isBuiltIn()) {
				return -1;
			}				
			if(isAtom && !other.isAtom()) {
				return -1;
			}
			return getLabel().compareTo(other.getLabel());	
		}
		return 0;
	}

	@Override
	public void apply(ITextViewer viewer, char trigger, int stateMask,
			int offset) {
		if (createArglist) {
			target2.apply(viewer, trigger, stateMask, offset);
		} else {
			try {
				viewer.getDocument().replace(this.offset, length, name);
			} catch (BadLocationException e) {
				Debug.report(e);
			}
		}
	}

	@Override
	public void selected(ITextViewer viewer, boolean smartToggle) {
		target2.selected(viewer, smartToggle);
	}

	@Override
	public void unselected(ITextViewer viewer) {
		target2.unselected(viewer);
	}

	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		return target2.validate(document, offset, event);
	}

	@Override
	public String getAdditionalProposalInfo() {
		return target2.getAdditionalProposalInfo();
	}

	@Override
	public IContextInformation getContextInformation() {
		return target2.getContextInformation();
	}

	@Override
	public Image getImage() {
		return target2.getImage();
	}

	@Override
	public Point getSelection(IDocument document) {
		return target2.getSelection(document);
	}
	
}
