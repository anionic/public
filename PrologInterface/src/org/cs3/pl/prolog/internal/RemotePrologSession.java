/*
 */
package org.cs3.pl.prolog.internal;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Hashtable;

import org.cs3.pl.prolog.SessionException;

/**
 */
public interface RemotePrologSession extends Remote {
	/**
	 * begins a query on the Prolog system. If there was an active query running, further
	 * results lost, so calls to next() will return results of this query. The
	 * resultant Hashtable contains keys equal to the unbound variables in the query,
	 * and values equal to their binding. If null is returned, no bindings could
	 * satisfy the query ("no"). If there were no unbound variables, an empty Hashtable
	 * is returned to signify "yes".
	 *
	 * Each Query <u>must</u> call the method checkPrologAccess() of the class
	 * PrologSynchronizer before attempting any interaction with the Prolog
	 * System. If there is an exclusive query active, the implementation
	 * must rethrow the resultant SessionException if it is caught.
	 *   
	 * @return a hashtable, containing the bindings generated.
	 * @param query a prolog query
	 * @throws IllegalStateException the session is disposed
	 * @throws SessionException an abnormal condition was detected
	 */
	
	public Hashtable query(String query) throws SessionException,RemoteException;
	public Hashtable[] queryAll(String query)throws SessionException,RemoteException;
	/**
	 * returns the next set of Bindings satisfying the last query.
	 * 
	 * Each Query <u>must</u> call the method checkPrologAccess() of the class
	 * PrologSynchronizer before attempting any interaction with the Prolog
	 * System. If there is an exclusive query active, the implementation
	 * must rethrow the resultant SessionException if it is caught.
	 * 
	 * @return another set of Bindings
	 * @throws IllegalStateException the session is disposed
	 * @throws SessionException an IO Error occured
	 */
	
	public Hashtable next() throws SessionException,RemoteException;
		
	public void shutdownServer(Boolean now)throws RemoteException;
}
