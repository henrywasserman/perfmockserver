package com.navteq.atg.mas;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.CopyOnWriteArrayList;


@SuppressWarnings("serial")
public class RuleInfoCollection extends CopyOnWriteArrayList<RuleInfo> {
	
	public RuleInfoCollection(String filePath) {
		super();
		
		File file = new File(filePath);
		init(file);
	}
	
	
	public RuleInfoCollection(File ruleDefFile) {
		super();
		
		init(ruleDefFile);
	}
	
	public RuleInfo getMatchRule(String reqQueryStr) throws NoMatchRuleException {
		if (reqQueryStr == null || reqQueryStr.isEmpty())
			throw new NoMatchRuleException("Null or empty query string");
		
		for (RuleInfo ruleInfo : this) {
			if (ruleInfo.matchQuery(reqQueryStr))
				return ruleInfo;
		}
		
		return null;
	}
	
	public String getXmlPath(String reqQueryStr) throws NoMatchRuleException {
		RuleInfo ruleInfo = getMatchRule(reqQueryStr);
		
		if (ruleInfo != null)
			return ruleInfo.getXmlPathStr();
		
		return "";
	}
	
	private void init(File ruleDefFile) {
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(ruleDefFile));
			
			String line = "";
			int lineNo = 1;
			
			while ((line = reader.readLine()) != null) {
				RuleInfo ruleInfo = null;
				int lc = lineNo;
				lineNo++;
				
				line = line.trim();
				
				if (line.isEmpty())
					continue;
				
				if (line.startsWith("#"))
					continue;
				
				String[] parts = line.split("\t");
				
				if (parts.length != 2 && parts.length != 3)
					continue;
				
				if (parts.length == 2) {
					ruleInfo = new RuleInfo(parts[0], parts[1], 0, lc);
					add(ruleInfo);
				}
				else if (parts.length == 3) {
					ruleInfo = new RuleInfo(parts[0], parts[1], Long.parseLong(parts[2]), lc );
					add(ruleInfo);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}
	}
}
