package com.navteq.atg.mas;

import java.util.regex.Pattern;

public class RuleKey {
	private String ruleKeyStr = "";
	private Pattern ruleKeyPattern = null;
	
	public RuleKey() {}
		
	public RuleKey(String keyStr) {
		if (keyStr != null) {
			ruleKeyStr = keyStr;
			ruleKeyPattern = Pattern.compile(ruleKeyStr);
		}
	}

	public String getRuleKeyStr() {
		return ruleKeyStr;
	}

	public void setRuleKeyStr(String ruleKeyStr) {
		this.ruleKeyStr = "";
		ruleKeyPattern = null;
		
		if (ruleKeyStr != null) {
			this.ruleKeyStr = ruleKeyStr;
			ruleKeyPattern = Pattern.compile(ruleKeyStr);
		}
	}

	public Pattern getRuleKeyPattern() {
		return ruleKeyPattern;
	}
	
	public boolean matches(String aRealKey) {
		return ruleKeyPattern.matcher(aRealKey).matches();
	}
	
	public boolean equals(Object another) {
		if (this == another)
			return true;
		
		if (!(another instanceof RuleKey))
			return false;
		
		return ((RuleKey) another).equals(ruleKeyStr);
	}
	
	public int hashCode() {
		return ruleKeyStr.hashCode();
	}
}
