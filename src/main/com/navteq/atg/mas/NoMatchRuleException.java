package com.navteq.atg.mas;

@SuppressWarnings("serial")
public class NoMatchRuleException extends Exception {
	public NoMatchRuleException() {
		this("No match rule");
	}
	public NoMatchRuleException(String msg) {
		super(msg);
	}
}
