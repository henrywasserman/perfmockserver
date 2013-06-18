package com.navteq.atg.mas;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleInfo {
	private String rulePatternStr = "";
	private String xmlPathPatternStr = "";
	private int lineNumber = 0;
	private String xmlPathStr = "";
	private long latency = 0;
	
	private Pattern rulePattern = null;
	
	
	public RuleInfo() {}

	public RuleInfo(String ruleDef, String xmlDef, long latency, int line) {
		this.latency = latency;
		if (ruleDef != null)
			rulePatternStr = ruleDef;
		
		if (xmlDef != null) {
			xmlPathPatternStr = xmlDef;
		}
			
		if (line >= 0)
			lineNumber = line;
		
		rulePattern = Pattern.compile(rulePatternStr);
	}

	
	public String getRulePatternStr() {
		return rulePatternStr;
	}

	public void setRulePatternStr(String rulePatternStr) {
		if (rulePatternStr != null)
			this.rulePatternStr = rulePatternStr;
		
		rulePattern = Pattern.compile(rulePatternStr);
	}

	public String getXmlPathStr() {
		return xmlPathStr;
	}

	public String getXmlPathPatternStr() {
		return xmlPathPatternStr;
	}

	public void setXmlPathPatternStr(String xmlPatternStr) {
		if (xmlPatternStr != null) {
			xmlPathPatternStr = xmlPatternStr;
		}
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public Pattern getRulePattern() {
		return rulePattern;
	}
	
	public Matcher getMatcher(String aQryStr) {
		Matcher ruleMatcher = rulePattern.matcher(aQryStr);
		if (ruleMatcher.matches())
			return ruleMatcher;
		else
			return null;
	}
	
	public boolean matchQuery(String aQryStr) {
		Matcher ruleMatcher = rulePattern.matcher(aQryStr);
		
		if (ruleMatcher == null)
			return false;
		
		
		if (!ruleMatcher.matches())
			return false;
		
		int gc = ruleMatcher.groupCount();
		
		if (gc == 0) {
			xmlPathStr = xmlPathPatternStr;
		}
		else
		{
			for (int i = 1; i <= gc; i++) {
				String gs = "${" + i + "}";
			
				if (xmlPathPatternStr.indexOf(gs) < 0)
					continue;
			
				String realGroup = ruleMatcher.group(i);
				xmlPathStr = xmlPathPatternStr.replace(gs, realGroup);
			}
		}
		
		return true;
	}
	
	public long getLatency() {
		return latency;
	}
	
	public String getMatchXmlFilePath(String aQryStr) {
		//call this method to get the xml path
		matchQuery(aQryStr);

		return xmlPathStr;
	}
	
	public String toString() {
		return "rule: " + rulePatternStr + ",\t" + "XML path: " + xmlPathStr + ",\t" + "line: " + lineNumber;
	}
}
