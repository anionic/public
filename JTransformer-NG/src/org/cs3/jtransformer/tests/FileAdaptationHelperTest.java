package org.cs3.jtransformer.tests;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.cs3.jtransformer.util.FileAdaptationHelper;
import org.cs3.jtransformer.util.JTConstants;
import org.cs3.jtransformer.util.JTUtils;

/**
 * 
 * @author Mark Schmatz
 *
 */
public class FileAdaptationHelperTest extends TestCase
{
	private FileAdaptationHelper fah = new FileAdaptationHelper();
	
	
	public void testAdaptFile()
	{
		String content = "agregoiuh pattern=\"test123\"";
		String expContent = "agregoiuh pattern=\"test123|.*\\.pl|.*\\.aj\"";
		Map regexPatternsWithNewStrings = new HashMap();
		regexPatternsWithNewStrings.put(
				"pattern=\"(.*?)\"",
				"pattern=\"" +
				"$1" +
				"|.*\\\\.pl" +
				"|.*\\\\.aj" +
				"\""
		);
		String newContent = fah.adaptContent(content, regexPatternsWithNewStrings);
		assertEquals(expContent, newContent);
		
		// ---
		
		content = "TestlllTest";
		expContent = "Test123lll321Test";
		regexPatternsWithNewStrings = new HashMap();
		regexPatternsWithNewStrings.put(
				"Test(.*?)Test",
				"Test123"+
				"$1" +
				"321Test"
		);
		newContent = fah.adaptContent(content, regexPatternsWithNewStrings);
		assertEquals(expContent, newContent);
		
		// ---
		
		content = "<classpathentry including=\"**/*.java|**/*.pl\"";
		expContent = "<classpathentry including=\"**/*.java|**/*.pl|**/" + JTConstants.CT_LIST_FILENAME + "|**/"+ JTConstants.FQCN_LIST_FILENAME +"\"";

		regexPatternsWithNewStrings = new HashMap();
		regexPatternsWithNewStrings.put(
				"\\<classpathentry\\s+?including=\"(.*?)\"",
				"<classpathentry including=\"" +
				"$1" +
				"|**/" + JTConstants.CT_LIST_FILENAME +
				"|**/" + JTConstants.FQCN_LIST_FILENAME +
				"\""
		);
		newContent = fah.adaptContent(content, regexPatternsWithNewStrings);
		assertEquals(expContent, newContent);

		// ---

		content =
			"Manifest-Version: 1.0\n" +
			"Bundle-Name: MarksTestAspectBundle\n" +
			"Bundle-SymbolicName: MarksTestAspectBundle\n" +
			"Bundle-Version: 1.0.0\n" +
			"Bundle-Activator: org.cs3.roots.test.schmatz.demo1.Activator\n" +
			"Import-Package: org.osgi.framework\n" +
			"Export-Package: org.cs3.roots.test.schmatz.demo1.aspects,\n" +
			"de.test123\n";

		expContent =
			"Manifest-Version: 1.0\n" +
			"Bundle-Name: MarksTestAspectBundle\n" +
			"Bundle-SymbolicName: MarksTestAspectBundle\n" +
			"Bundle-Version: 1.0.0\n" +
			"Bundle-Activator: org.cs3.roots.test.schmatz.demo1.Activator\n" +
			"Import-Package: org.osgi.framework\n" +
			"Export-Package: "+JTConstants.RESOURCES_FILELISTS_PACKAGE+", org.cs3.roots.test.schmatz.demo1.aspects,\n" +
			"de.test123\n";

		regexPatternsWithNewStrings = new HashMap();
		regexPatternsWithNewStrings.put(
				"Export-Package:(.*)",
				"Export-Package: " +
				JTConstants.RESOURCES_FILELISTS_PACKAGE + "," +
				"$1"
		);
		newContent = fah.adaptContent(content, regexPatternsWithNewStrings, JTConstants.RESOURCES_FILELISTS_PACKAGE + ",");
		assertEquals(expContent, newContent);
		
		// ---

		content =
			"Manifest-Version: 1.0\n" +
			"Bundle-Name: MarksTestAspectBundle\n" +
			"Bundle-SymbolicName: MarksTestAspectBundle\n" +
			"Bundle-Version: 1.0.0\n" +
			"Bundle-Activator: org.cs3.roots.test.schmatz.demo1.Activator\n" +
			"Import-Package: org.osgi.framework\n" +
			"Export-Package: "+JTConstants.RESOURCES_FILELISTS_PACKAGE+", org.cs3.roots.test.schmatz.demo1.aspects,\n" +
			"de.test123\n";

		expContent =
			"Manifest-Version: 1.0\n" +
			"Bundle-Name: MarksTestAspectBundle\n" +
			"Bundle-SymbolicName: MarksTestAspectBundle\n" +
			"Bundle-Version: 1.0.0\n" +
			"Bundle-Activator: org.cs3.roots.test.schmatz.demo1.Activator\n" +
			"Import-Package: org.osgi.framework\n" +
			"Export-Package: "+JTConstants.RESOURCES_FILELISTS_PACKAGE+", org.cs3.roots.test.schmatz.demo1.aspects,\n" +
			"de.test123\n";

		regexPatternsWithNewStrings = new HashMap();
		regexPatternsWithNewStrings.put(
				"Export-Package:(.*)",
				"Export-Package: " +
				JTConstants.RESOURCES_FILELISTS_PACKAGE + "," +
				"$1"
		);
		newContent = fah.adaptContent(content, regexPatternsWithNewStrings, JTConstants.RESOURCES_FILELISTS_PACKAGE);
		assertEquals(expContent, newContent);

		// ---
		
		String expContent2 =
			"Manifest-Version: 1.0\n" +
			"Bundle-Name: MarksTestAspectBundle\n" +
			"Bundle-SymbolicName: MarksTestAspectBundle\n" +
			"Bundle-Version: 1.0.0\n" +
			"Bundle-Activator: org.cs3.roots.test.schmatz.demo1.Activator\n" +
			"Import-Package: org.osgi.framework\n" +
			"Export-Package: "+JTConstants.RESOURCES_FILELISTS_PACKAGE+", "+JTConstants.RESOURCES_FILELISTS_PACKAGE+", org.cs3.roots.test.schmatz.demo1.aspects,\n" +
			"de.test123\n";
		newContent = fah.adaptContent(content, regexPatternsWithNewStrings);
		assertEquals(expContent2, newContent);
		
		// ---

		content =
			"Manifest-Version: 1.0\n" +
			"Bundle-Name: MarksTestAspectBundle\n" +
			"Bundle-SymbolicName: MarksTestAspectBundle\n" +
			"Bundle-Version: 1.0.0\n" +
			"Bundle-Activator: org.cs3.roots.test.schmatz.demo1.Activator\n" +
			"Import-Package: org.osgi.framework\n";

		expContent =
			"Manifest-Version: 1.0\n" +
			"Bundle-Name: MarksTestAspectBundle\n" +
			"Bundle-SymbolicName: MarksTestAspectBundle\n" +
			"Bundle-Version: 1.0.0\n" +
			"Bundle-Activator: org.cs3.roots.test.schmatz.demo1.Activator\n" +
			"Import-Package: org.osgi.framework\n\n" +
			"Export-Package: "+JTConstants.RESOURCES_FILELISTS_PACKAGE+", org.cs3.roots.test.schmatz.demo1.aspects,\n" +
			"de.test123\n";

		regexPatternsWithNewStrings = new HashMap();
		regexPatternsWithNewStrings.put(
				"^(.*)$",
				"$1\n" +
				"Export-Package: "+JTConstants.RESOURCES_FILELISTS_PACKAGE+", org.cs3.roots.test.schmatz.demo1.aspects,\n" +
				"de.test123\n"
		);
		newContent = fah.adaptContent(content, regexPatternsWithNewStrings, JTConstants.RESOURCES_FILELISTS_PACKAGE);
		assertEquals(expContent, newContent);
		
		content = "<classpathentry exported=\"true\" kind=\"lib\" path=\"lib/DaffodilDB_Common.jar\"/>\n" +
				  "<classpathentry kind=\"lib\" path=\"lib/lib2.jar\"/>";
		expContent = "<classpathentry exported=\"true\" kind=\"lib\" path=\"/MarksTestAspectBundle/lib/DaffodilDB_Common.jar\"/>\n" +
				     "<classpathentry kind=\"lib\" path=\"/MarksTestAspectBundle/lib/lib2.jar\"/>";

		regexPatternsWithNewStrings = new HashMap();
		regexPatternsWithNewStrings.put(
				"(<classpathentry .*?kind=\"lib\" .*?path=\")([^/].*?\"/>)",
						"$1/" + "MarksTestAspectBundle" + "/$2");
		newContent = fah.adaptContent(content, regexPatternsWithNewStrings);
		assertEquals(expContent, newContent);

	}
	
//	public void testCTPackageExtractor()
//	{ 
//		List list = new ArrayList();
//		list.add("'org/cs3/roots/test/schmatz/demo1/aspects/org/cs3/roots/test/schmatz/demo1/aspects/DemoAspect__default__37_i'(Jp0) ##--## org.cs3.roots.test.schmatz.demo1.aspects.DemoAspect__default__37_i");
//		list.add("'org/cs3/roots/test/schmatz/demo1/aspects/org/cs3/roots/test/schmatz/demo1/aspects/Flip_reverese_103_i'(Jp1) ##--## org.cs3.roots.test.schmatz.demo1.aspects.Flip_reverese_103_i");
//		list.add("'org/cs4711/roots/test/schmatz/demo1/aspects/org/cs3/roots/test/schmatz/demo1/aspects/Flip_reverese_103_i'(Jp1) ##--## org.cs3.roots.test.schmatz.demo1.aspects.Flip_reverese_103_i");
//		list.add("'org/cs3/roots/test/schmatz/demo1/aspects/org/cs3/roots/test/schmatz/demo1/aspects/Flip_reverese_103_i'(Jp1) ##--## org.cs3.roots.test.schmatz.demo1.aspects.Flip_reverese_103_i");
//		
//		String subDirForCts = JTUtils.makeItAPackagePart(JTConstants.SUBDIR_FOR_CTS);
//		String exp = subDirForCts + "org.cs3.roots.test.schmatz.demo1.aspects, " + subDirForCts + "org.cs4711.roots.test.schmatz.demo1.aspects";
//			
//		String str = JTUtils.getCTPackagesAsCSV(list);
//		
//		assertEquals(exp, str);
//	}
	
	public void testEmptyLine()
	{
		String str = "\ntest1\n\ntest2\n\n\ntest3\n\n\n\n";
		
		assertEquals("\ntest1\ntest2\ntest3\n", JTUtils.removeEmptyLines(str));
	}
}
