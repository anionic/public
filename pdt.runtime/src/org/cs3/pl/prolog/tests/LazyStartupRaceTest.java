package org.cs3.pl.prolog.tests;

import junit.framework.TestCase;

import org.cs3.pl.common.logging.Debug;
import org.cs3.pl.prolog.PrologInterface;
import org.cs3.pl.prolog.PrologInterfaceException;
import org.cs3.pl.prolog.internal.AbstractPrologInterface;

public class LazyStartupRaceTest extends TestCase {
	 private PrologInterface pif;

	@Override
	protected void setUp() throws Exception {
         Debug.setDebugLevel(Debug.LEVEL_DEBUG);
	     
//	       pif=PrologInterfaceFactory.newInstance().create();
	      pif=AbstractPrologInterface.newInstance();
	      
	    }
	    
	    /* (non-Javadoc)
	     * @see junit.framework.TestCase#tearDown()
	     */
	    @Override
		protected void tearDown() throws Exception {
	        pif.stop();
	    }
	    
	    public void testLazyStartUp() throws PrologInterfaceException {
	    	pif.getSession();

		}
}
