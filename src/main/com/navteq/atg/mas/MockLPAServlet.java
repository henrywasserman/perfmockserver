package com.navteq.atg.mas;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


@SuppressWarnings("serial")
public class MockLPAServlet extends HttpServlet {
	private static String lineSeparator = System.getProperty("line.separator");
	
	private RuleInfoCollection ruleCollection = null;
	private ClassLoader classLoader = getClass().getClassLoader();
	private Map<String, String> fmMap = null;
	private String defaultPageXml = "";
	private String errorPageXml = "";
	
	private String errorPageStr = "";
	private String soapPageStr = "";
	
	private String getPageTitle = "";
	private String postPageTitle = "";
	private String soapPageTitle = "";

	private String postQueryString = "";
	
	private Logger logger = null;

	
	public void init(ServletConfig cfg) throws ServletException {
		super.init(cfg);
		
		logger = Logger.getLogger(MockLPAServlet.class);
		
		ServletContext context = cfg.getServletContext();
		
		getPageTitle = context.getInitParameter("get-page-title");
		postPageTitle = context.getInitParameter("post-page-title");
		soapPageTitle = context.getInitParameter("soap-page-title");
		
		classLoader = getClass().getClassLoader();
		
		//load rule definitions
		String ruleDefPath = context.getInitParameter("rule-def-file");
		URL url = classLoader.getResource(ruleDefPath);
		try {
			File ruleDefFile = new File(url.toURI());
			
			ruleCollection = new RuleInfoCollection(ruleDefFile);
		} catch (Exception e) {}
		
		//load field mapping file
		fmMap = new HashMap<String, String>();
		String fmDefPath = context.getInitParameter("field-mapping-file");
		url = classLoader.getResource(fmDefPath);
		try {
			File fmFile = new File(url.toURI());
			if (fmFile.exists())
				readFiledMappingFile(fmFile);
		} catch (Exception e) {}
		
		//pre-load the default xml and error page xml and keep them in memory.
		
		//By doing this it will use a little bit more memory (we keep the default
		//and error page xml as string. As these 2 pages/files should not be
		//big---maybe less then 10KB for each of them, so the memory footprint
		//should be minimal. And by keeping them in memory, we don't need to load
		//them from disk to memory everytime they are needed
		String dftPagePath = context.getInitParameter("default-response-file");
		if (dftPagePath == null || dftPagePath.isEmpty())
			dftPagePath = "default.xml";
		
		url = classLoader.getResource(dftPagePath);
		try {
			File dftPageFile = new File(url.toURI());
			defaultPageXml = readFile(dftPageFile);
		} catch (Exception e) {}
		
		String errorPagePath = context.getInitParameter("error-response-file");
		if (errorPagePath == null || errorPagePath.isEmpty())
			errorPagePath = "error.xml";
		
		url = classLoader.getResource(errorPagePath);
		try {
			File errorPageFile = new File(url.toURI());
			errorPageXml = readFile(errorPageFile);
		} catch (Exception e) {}
		
		File errorPageFile = new File(context.getRealPath("/invalid-request.html"));
		errorPageStr = readFile(errorPageFile);
		
		File soapPageFile = new File(context.getRealPath("/soap-request-not-supported.html"));
		soapPageStr = readFile(soapPageFile);
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException {
		String contextPath = req.getContextPath();
		String reqUri = req.getRequestURI();
		String reqUrl = req.getRequestURL().toString();
		String encQueryStr = "";
		RuleInfo ruleInfo = null;
		if (postQueryString.isEmpty()) {
			encQueryStr = req.getQueryString();
		}
		else {
			encQueryStr = postQueryString;
		}
			
		
		//log4j is thread safe. So we can group all logging info for one request together
		//so that user will not get lost in a multi threaded environment. 
		//The list will have 2 elements: first is the logging message, second is the
		//exception is any when the message is created 
		CopyOnWriteArrayList<Object[]> logInfoHolder = new CopyOnWriteArrayList<Object[]>();
		
		//log the full URL
		Object[] entry = new Object[2];
		entry[0] = reqUrl + "?" + encQueryStr;
		entry[1] = null;
		
		logInfoHolder.add(entry);
		
		Level level = Level.DEBUG;
		
		try {
			String pp = reqUri;
			try {
				pp = reqUri.substring(contextPath.length() + 1);
			} catch (Exception e) {}
							
			if (pp.startsWith(soapPageTitle)) {
				String respStr = soapPageStr;
				
				resp.getWriter().write(respStr);
				
				return;
			}
			String decQueryStr = "";
			
			if (encQueryStr != null)
				decQueryStr = reqUrl + "/" + URLDecoder.decode(encQueryStr, "UTF-8");
			
			File xmlFile = null;
			File headerFile = null;
			
			boolean isDefault = false;
			boolean isError = false;
			
			try {
				ruleInfo = ruleCollection.getMatchRule(decQueryStr);
				
				if (ruleInfo == null)
					isDefault = true;
				else {
					entry = new Object[2];
					entry[0] = "Matching rule: " + "\t" + ruleInfo.toString();
					entry[1] = null;
					logInfoHolder.add(entry);
					
					//stb.append("Matching rule:" + lineSeparator);
					String xmlFileStr = ruleInfo.getXmlPathStr();
					
					URL url = classLoader.getResource(xmlFileStr);
					try {
						xmlFile = new File(url.toURI());
					} catch (Exception e) {
						xmlFile = null;
					}
					
					if (xmlFile == null || !xmlFile.exists()) {
						isDefault = true;
						if (Level.INFO.isGreaterOrEqual(level))
							level = Level.INFO;
						
						entry = new Object[2];
						entry[0] = "Could not find " + xmlFileStr + ". Use default response XML instead.";
						entry[1] = null;
						logInfoHolder.add(entry);
					} else {
						String headerFileStr = xmlFileStr.replace(".xml", ".headers");
						url = classLoader.getResource(headerFileStr);
						try {
							headerFile = new File(url.toURI());
						} catch (Exception e) {
							headerFile = null;
						}
						
						if (headerFile == null || !headerFile.exists()) {
							if (Level.INFO.isGreaterOrEqual(level))
								level = Level.INFO;
							
							entry = new Object[2];
							entry[0] = "Could not find header file " + headerFileStr + ".";
							entry[1] = null;
							logInfoHolder.add(entry);
						}
					}
					
				}
			} catch (Exception e) {
				isError = true;
			}
			
			//handle default or error page
			if (isDefault || isError) {
				String xmlStr = defaultPageXml;
				if (isError)
					xmlStr = errorPageXml;
				
				resp.getWriter().write(
						xmlStr.replace("<!--REQUEST-->",
							"<![CDATA[" + reqUrl + "/" + encQueryStr + "]]>"));
				
				return;
			}
			
			try {
				writerResponseHeader(headerFile, resp);
				Thread.sleep(ruleInfo.getLatency());
				writeResponseXml(xmlFile, req, resp, logInfoHolder);
			} catch (Exception e) {
				if (Level.ERROR.isGreaterOrEqual(level))
					level = Level.ERROR;
				
				entry = new Object[2];
				entry[0] = e.getMessage();
				entry[1] = e;
				logInfoHolder.add(entry);
				
				e.printStackTrace();
				throw new ServletException(e.getCause());
			}
		} finally {
			//make sure that logging for the same request goes together
			synchronized(MockLPAServlet.class) {
				for (Object[]  logEntry: logInfoHolder) {
					String msg = (String) logEntry[0];
					Throwable t = logEntry[1] == null ? null : (Throwable) logEntry[1];
					
					if (t == null)
						logger.log(level, msg);
					else
						logger.log(level, msg, t);
					
				}
				
				logger.log(level, lineSeparator);
			}
			try {
				if (resp != null) {
					resp.flushBuffer();
					if (resp.getWriter() != null) {
						resp.getWriter().close();
					}	
				}
			}
			catch (Exception e) {}
		}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException {
		
		
		postQueryString = mapToString((Map <String,String[]>)req.getParameterMap());
		
		doGet(req, resp);
	}
	
	private String mapToString(Map<String, String[]> map) {
		   StringBuilder stringBuilder = new StringBuilder();  
		   
		   for (String key : map.keySet()) {  
		    if (stringBuilder.length() > 0) {  
		     stringBuilder.append("&");  
		    }  
		    String value = map.get(key)[0];  
		    try {  
		     stringBuilder.append((key != null ? URLEncoder.encode(key, "UTF-8") : ""));  
		     stringBuilder.append("=");  
		     stringBuilder.append(value != null ? URLEncoder.encode(value, "UTF-8") : "");  
		    } catch (UnsupportedEncodingException e) {  
		     throw new RuntimeException("This method requires UTF-8 encoding support", e);  
		    }  
		   }  
		  
		   return stringBuilder.toString();  
		  }  
	
	private void readFiledMappingFile(File fmFile) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(fmFile));
		
		try {
			String line = "";
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				
				if (line.isEmpty() || line.startsWith("#"))
					continue;
				
				String[] kv = line.split("=");
				
				if (kv.length != 2)
					continue;
				
				kv[0] = kv[0].trim();
				kv[1] = kv[1].trim();
				
				if (kv[0].isEmpty() || kv[1].isEmpty())
					continue;
				
				fmMap.put(kv[0], kv[1]);
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}
	}
	
	private String readFile(File file) {
		if (file == null || !file.exists())
			return "";
		
		FileInputStream fin = null;
		
		try {
			fin = new FileInputStream(file);
			
			byte[] buf = new byte[102400]; //100K
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream(102400);
			
			int num = -1;
			
			while ((num = fin.read(buf, 0, 102400)) >= 0)
				bos.write(buf, 0, num);
			
			return bos.toString();
		} catch (Exception e) {
			return "";
		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (Exception e) {}
			}
		}
	}
	
	private void writerResponseHeader(File headerDefFile, HttpServletResponse resp) throws Exception {
		if (headerDefFile == null || !headerDefFile.exists())
			return;
		
		BufferedReader reader = new BufferedReader(new FileReader(headerDefFile));
		
		String line = "";
		
		try {
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty())
					continue;
				
				int cindex = line.indexOf(":");
				if (cindex <= 0 || cindex >= line.length() - 1)
					continue;
				
				String key = line.substring(0, cindex);
				String value = line.substring(cindex + 1);
				
				key = key.trim();
				value = value.trim();
				
				if (key.isEmpty() || value.isEmpty())
					continue;
				
				resp.setHeader(key, value);
			}
		} catch (Exception ex) {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}
	}
	
	private void writeResponseXml(File xmlFile, HttpServletRequest req, HttpServletResponse resp, CopyOnWriteArrayList<Object[]> logInfoHolder) throws Exception {
		Document doc = (DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile));
		processXml(req, doc, logInfoHolder);
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);

		String xmlStr = result.getWriter().toString();
		
		resp.setContentType("application/xml");
		resp.getWriter().write(xmlStr);
	}
	
	private void processXml(HttpServletRequest req, Document doc, CopyOnWriteArrayList<Object[]> logInfoHolder) throws Exception {
		Element root = doc.getDocumentElement();
		
		//XPath and XPathFactory are both not thread-safe so they cannot be shared
		//by multiple threads without synchronization. We could create a global XPathFactory
		//instance and synchronize when creating the XPath instance. This way we may
		//gain on removing the XPathFactory instance creation and reduce the memory
		//usage at peak time but we will have to have threads wait on the XPath creation
		//and thus lose performance. It is hard to determine at this time which way has
		//more benefits and it's more likely having local XPathFactory instance would be
		//more efficient, we do it now. And if later we find the other way is better, we
		//can change to it
		
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		Iterator<Map.Entry<String, String>> iter = fmMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, String> entry = iter.next();
			String fieldName = entry.getKey();
			String path = entry.getValue();
			
			String fieldValue = req.getParameter(fieldName);
			if (fieldValue == null || fieldValue.isEmpty())
				continue;
			
			Element repEle = (Element) xpath.evaluate(path, root, XPathConstants.NODE);
			if (repEle != null) {
				
				NodeList nodeList = repEle.getChildNodes();
				if (nodeList == null || nodeList.getLength() == 0)
					continue;
				
				String tagName = repEle.getTagName();
				String origValue = "";
				
				Node repNode = null;
				
				for (int i = 0; i < nodeList.getLength(); i++) {
					Node node = nodeList.item(i);
					if (node.getNodeType() == Node.COMMENT_NODE)
						continue;
					
					repNode = node;
					origValue = repNode.getTextContent();
					if (origValue == null)
						origValue = "";
					break;
				}
				
				if (repNode == null)
					continue;
				
				repNode.setTextContent(fieldValue);
				
				Object[] logEntry = new Object[2];
				logEntry[0] = "for tag <" + tagName + ">, replace " + origValue + " with " + fieldValue;
				logEntry[1] = null;
				logInfoHolder.add(logEntry);
			}
		}
	}
} 
