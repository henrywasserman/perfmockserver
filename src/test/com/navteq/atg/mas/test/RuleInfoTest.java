package com.navteq.atg.mas.test;

import com.navteq.atg.mas.RuleInfo;

import junit.framework.TestCase;

public class RuleInfoTest extends TestCase {
	public void testRuleInfo() {
		RuleInfo ruleInfo = new RuleInfo(".*keywords=(testcase\\d*).*", "${1}.xml", 5);
		//String key = "keywords";
		String reqUrl = "http://localhost:8080/lpa/api1.aspx?a=endusercheckin&enduserid=3413755&longitude=-71058716&latitude=42362636&localepref=en-us&keywords=testcase123&responsetype=xml&ver=2.2";
		String xmlFileName = "testcase123.xml";
		
		String calcXmlFileName = ruleInfo.getMatchXmlFilePath(reqUrl);
		
		assertEquals(xmlFileName, calcXmlFileName);
	}
}
