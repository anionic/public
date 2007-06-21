package org.cs3.pdt.internal.views;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.cs3.pdt.runtime.PrologRuntime;
import org.cs3.pdt.runtime.PrologRuntimePlugin;
import org.cs3.pl.common.Debug;
import org.cs3.pl.common.Util;
import org.cs3.pl.cterm.CCompound;
import org.cs3.pl.cterm.CTerm;
import org.cs3.pl.metadata.Predicate;
import org.cs3.pl.prolog.AsyncPrologSession;
import org.cs3.pl.prolog.AsyncPrologSessionEvent;
import org.cs3.pl.prolog.DefaultAsyncPrologSessionListener;
import org.cs3.pl.prolog.LifeCycleHook2;
import org.cs3.pl.prolog.PLUtil;
import org.cs3.pl.prolog.PrologEventDispatcher;
import org.cs3.pl.prolog.PrologInterface;
import org.cs3.pl.prolog.PrologInterface2;
import org.cs3.pl.prolog.PrologInterfaceEvent;
import org.cs3.pl.prolog.PrologInterfaceException;
import org.cs3.pl.prolog.PrologInterfaceListener;
import org.cs3.pl.prolog.PrologLibraryManager;
import org.cs3.pl.prolog.PrologSession;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.properties.IPropertySource;

public abstract class ContentModel extends DefaultAsyncPrologSessionListener
		implements PrologFileContentModel, LifeCycleHook2,
		PrologInterfaceListener {

	private static final String HOOK_ID = "PrologFileContentModelHook";

	private Object input;

	private PrologInterface pif;

	private HashMap cache = new HashMap();

	private String oneMomentPlease = "one moment please...";

	

	private Vector listeners = new Vector();

	private HashMap specificListeners = new HashMap();

	private long timestamp = Long.MIN_VALUE;

	private PrologEventDispatcher dispatcher;

	private String subject;

	public void update(PrologInterfaceEvent e) {

		String subject = e.getSubject();
		String event = e.getEvent();

		Debug.debug("ContentModel received event: subject=" + subject
				+ ", event=" + event);
		if("invalid".equals(event)){
			try {
				reset();
			} catch (PrologInterfaceException e1) {
				Debug.rethrow(e1);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.internal.views.PrologFileContentModel#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object parentElement) {
		if (parentElement instanceof PEFNode) {
			String type = ((PEFNode) parentElement).getType();
			return "pef_file".equals(type) || "pef_predicate".equals(type);
		}
		return parentElement == input;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.internal.views.PrologFileContentModel#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement)
			throws PrologInterfaceException {
		Debug.debug("outline getChildren for " + parentElement);
		Vector children = null;
		synchronized (cache) {
			children = getCachedChildren(parentElement);
			if (children.isEmpty()) {
				Debug.debug("outline no children cached for " + parentElement);
				if (parentElement instanceof CTermNode) {
					// FIXME: handle partial fetched terms (not implemented yet)
					CTerm term = ((CTermNode) parentElement).term;
					CCompound c = null;
					if (term instanceof CCompound) {
						c = (CCompound) term;
						for (int i = 0; i < c.getArity(); i++) {
							children.add(new CTermNode(c.getArgument(i)));
						}
					}
				} else if (pif != null && pif.isUp()) {
					Debug.debug("outline adding oneMomentPlease to "
							+ parentElement);
					children.add(oneMomentPlease);

					fetchChildren(parentElement);
				}
			} else {
				Debug.debug("outline found cached children "
						+ Util.prettyPrint(children));
			}
		}
		return children.toArray();
	}

	private void fetchChildren(Object parentElement)
			throws PrologInterfaceException {

		if (parentElement instanceof PEFNode) {
			fetchChildren((PEFNode) parentElement);

		} else if (parentElement == input) {
			fetchChildren(getFile());
		}
	}

	private void fetchChildren(File file) throws PrologInterfaceException {
		final AsyncPrologSession session = getSession();
		String query = "get_pef_file('"
				+ Util.prologFileName(file)
				+ "',FID),"
				+ "pdt_outline_child(FID,pef_file,FID,ChildT,Child),"
				+ "pdt_outline_label(FID,ChildT,Child,Label),"
				+ "findall(Tag,pdt_outline_tag(FID,ChildT,Child,Tag),Tags),"
				+ "(	pdt_outline_position(FID,ChildT,Child,Start,End)->true;Start= -1,End= -1)";
		session.queryAll(input, query);
		disposeWhenDone(session);
	}

	private void disposeWhenDone(final AsyncPrologSession session) {
		new Thread(){
			@Override
			public void run() {
				try {
					session.join();
				} catch (PrologInterfaceException e) {
					Debug.report(e);
				}
				session.dispose();
			}
		}.start();
	}

	private void fetchChildren(PEFNode parent) throws PrologInterfaceException {
		AsyncPrologSession session = getSession();
		String query = "get_pef_file('"
				+ Util.prologFileName(getFile())
				+ "',FID),"
				+ "pdt_outline_child(FID,"
				+ parent.getType()
				+ ","
				+ parent.getId()
				+ ",ChildT,Child),"
				+ "pdt_outline_label(FID,ChildT,Child,Label),"
				+ "findall(Tag,pdt_outline_tag(FID,ChildT,Child,Tag),Tags),"
				+ "(	pdt_outline_position(FID,ChildT,Child,Start,End)->true;Start= -1,End= -1)";
		session.queryAll(parent, query);
		disposeWhenDone(session);
	}

	private static class _PEFNode implements PEFNode, IAdaptable {

		private int endPosition;

		private File file;

		private int id;

		private String label;

		private int startPosition;

		private Set tags;

		private String type;

		public _PEFNode(AsyncPrologSessionEvent e, File file) {
			type = (String) e.bindings.get("ChildT");
			id = Integer.parseInt((String) e.bindings.get("Child"));
			startPosition = Integer.parseInt((String) e.bindings.get("Start"));
			endPosition = Integer.parseInt((String) e.bindings.get("End"));
			this.file = file;
			label = (String) e.bindings.get("Label");
			tags = new HashSet();
			tags.addAll((Collection) e.bindings.get("Tags"));
		}

		public Object getAdapter(Class adapter) {
			if (IPropertySource.class.isAssignableFrom(adapter)) {
				return new PEFNodePropertySource(this);
			}
			return null;
		}

		@Override
		public String toString() {
			return label;
		}

		public int getEndPosition() {
			return endPosition;
		}

		public File getFile() {
			return this.file;
		}

		public int getId() {

			return this.id;
		}

		public String getLabel() {
			return this.label;
		}

		public int getStartPosition() {
			return this.startPosition;
		}

		public Set getTags() {

			return this.tags;
		}

		public String getType() {
			return this.type;
		}

	}

	public void goalHasSolution(AsyncPrologSessionEvent e) {
		Object parent = e.ticket;
		PEFNode p = new _PEFNode(e, getFile());
		addChild(parent, p);
	}

	private AsyncPrologSession getSession() throws PrologInterfaceException {
		if ( pif != null) {
			AsyncPrologSession session = ((PrologInterface2) pif).getAsyncSession();
			// session.setPreferenceValue("socketsession.canonical", "true");
			session.addBatchListener(this);
			return session;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.internal.views.PrologFileContentModel#setFile(java.io.File)
	 */
	public void setFile(File file) throws IOException, PrologInterfaceException {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.internal.views.PrologFileContentModel#setPif(org.cs3.pl.prolog.PrologInterface)
	 */
	public void setPif(PrologInterface pif, PrologEventDispatcher d)
			throws PrologInterfaceException {
		
		if (this.dispatcher != null&&this.subject!=null) {
			this.dispatcher.removePrologInterfaceListener(this.subject, this);
		}
		if (this.pif != null) {
			((PrologInterface2) this.pif).removeLifeCycleHook(this, HOOK_ID);
		}
		this.pif = pif;
		this.dispatcher=d;
		if (this.pif != null) {
			this.pif.addLifeCycleHook(this, HOOK_ID, new String[0]);
			if (pif.isUp()) {
				afterInit(pif);
			}
		}
		if (this.dispatcher != null&&this.subject!=null) {
			this.dispatcher.addPrologInterfaceListener(this.subject, this);
		}
		reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.internal.views.PrologFileContentModel#getPif()
	 */
	public PrologInterface getPif() {
		return pif;
	}

	public Object getInput() {

		return input;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.cs3.pdt.internal.views.PrologFileContentModel#reset()
	 */
	public void reset() throws PrologInterfaceException {
		synchronized (this) {
			timestamp = System.currentTimeMillis();
		}
		

		synchronized (cache) {
			cache.clear();

		}

		fireContentModelChanged();
	}

	private void addChildren(Object parent, Collection collection) {
		Vector children = null;
		Object[] removed = null;
		synchronized (cache) {
			children = getCachedChildren(parent);
			if (children.contains(oneMomentPlease)) {
				removed = children.toArray();
				children.clear();

			}
			children.addAll(collection);
		}
		fireChildrenAdded(parent, collection.toArray());
		if (removed != null) {
			fireChildrenRemoved(parent, removed);
		}

	}

	private void addChild(Object parent, Object child) {
		Vector children = new Vector();
		children.add(child);
		addChildren(parent, children);
	}

	private Vector getCachedChildren(Object parent) {
		synchronized (cache) {
			Vector children = (Vector) cache.get(parent);
			if (children == null) {
				children = new Vector();

				cache.put(parent, children);
			}
			return children;
		}

	}

	private void fireChildrenAdded(Object parent, Object[] children) {
		PrologFileContentModelEvent e = new PrologFileContentModelEvent(this,
				parent, children, getLastResetTime());
		HashSet clone = new HashSet();
		synchronized (listeners) {
			clone.addAll(listeners);
		}
		synchronized (specificListeners) {
			clone.addAll(getListenersForParent(parent));
		}
		for (Iterator iter = clone.iterator(); iter.hasNext();) {
			PrologFileContentModelListener l = (PrologFileContentModelListener) iter
					.next();
			l.childrenAdded(e);
		}

	}

	private void fireChildrenRemoved(Object parent, Object[] children) {
		PrologFileContentModelEvent e = new PrologFileContentModelEvent(this,
				parent, children, getLastResetTime());
		HashSet clone = new HashSet();
		synchronized (listeners) {
			clone.addAll(listeners);
		}
		synchronized (specificListeners) {
			clone.addAll(getListenersForParent(parent));
		}
		for (Iterator iter = clone.iterator(); iter.hasNext();) {
			PrologFileContentModelListener l = (PrologFileContentModelListener) iter
					.next();
			l.childrenRemoved(e);
		}

	}

	private void fireContentModelChanged() {

		PrologFileContentModelEvent e = new PrologFileContentModelEvent(this,
				getLastResetTime());
		HashSet clone = new HashSet();
		synchronized (listeners) {
			clone.addAll(listeners);
		}
		synchronized (specificListeners) {
			for (Iterator iter = specificListeners.values().iterator(); iter
					.hasNext();) {
				Collection c = (Collection) iter.next();
				clone.addAll(c);
			}
		}
		for (Iterator iter = clone.iterator(); iter.hasNext();) {
			PrologFileContentModelListener l = (PrologFileContentModelListener) iter
					.next();
			l.contentModelChanged(e);
		}
	}

	public void addPrologFileContentModelListener(
			PrologFileContentModelListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}

	}

	public void removePrologFileContentModelListener(
			PrologFileContentModelListener l) {
		if (listeners.contains(l)) {
			listeners.remove(l);
		}

	}

	public void setInput(Object input) {
		this.input = input;
		try {
			if (this.dispatcher != null && this.subject != null) {

				this.dispatcher.removePrologInterfaceListener(this.subject,
						this);

			}
			this.subject = "builder(outline('" + Util.prologFileName(getFile())
					+ "'))";
			if (this.dispatcher != null && this.subject != null) {

				this.dispatcher.addPrologInterfaceListener(this.subject, this);

			}

		} catch (PrologInterfaceException e) {
			Debug.rethrow(e);
		}
	}

	public Vector getListenersForParent(Object parent) {
		synchronized (specificListeners) {
			Vector listeners = (Vector) specificListeners.get(parent);
			if (listeners == null) {
				listeners = new Vector();
				specificListeners.put(parent, listeners);
			}
			return listeners;
		}
	}

	public void addPrologFileContentModelListener(Object parent,
			PrologFileContentModelListener l) {
		synchronized (specificListeners) {
			Vector listeners = getListenersForParent(parent);
			if (!listeners.contains(l)) {
				listeners.add(l);
			}
		}

	}

	public void removePrologFileContentModelListener(Object parent,
			PrologFileContentModelListener l) {
		synchronized (specificListeners) {
			Vector listeners = getListenersForParent(parent);
			if (listeners.contains(l)) {
				listeners.remove(l);
			}
		}
	}

	public void afterInit(PrologInterface pif) throws PrologInterfaceException {
		PrologLibraryManager mgr = PrologRuntimePlugin.getDefault()
				.getLibraryManager();
		PrologSession s = pif.getSession();
		try {
			PLUtil.configureFileSearchPath(mgr, s,
					new String[] { PrologRuntime.LIB_PDT });
			s.queryOnce("use_module(library('facade/pdt_outline'))");
			s.queryOnce("use_module(library('pef/pef_api'))");
		} finally {
			if (s != null) {
				s.dispose();
			}
		}
		reset();
	}

	public void beforeShutdown(PrologInterface pif, PrologSession s)
			throws PrologInterfaceException {
		
		reset();

	}

	public void onError(PrologInterface pif) {
		
		try {
			reset();
		} catch (PrologInterfaceException e) {
			Debug.rethrow(e);
		}
	}

	public void onInit(PrologInterface pif, PrologSession initSession)
			throws PrologInterfaceException {
		;

	}

	public void dispose() {

		try {
			setPif(null, null);
		} catch (PrologInterfaceException e) {
			Debug.rethrow(e);
		}
		cache.clear();
		listeners.clear();
		//listeners = null;
		specificListeners.clear();
		//specificListeners = null;
		//input = null;

	}

	public synchronized long getLastResetTime() {
		return timestamp;
	}

}
