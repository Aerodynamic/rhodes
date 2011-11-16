/*------------------------------------------------------------------------
* (The MIT License)
* 
* Copyright (c) 2008-2011 Rhomobile, Inc.
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
* 
* http://rhomobile.com
*------------------------------------------------------------------------*/

package com.rho;

import java.util.Calendar;

import net.rim.device.api.system.EventLogger;
import java.util.Vector;

public class RhoLogger {
	public static final boolean RHO_STRIP_LOG = false;
	public static final long EVENT_GUID = 0x4c9c8411d87982f2L;

	private static final int L_TRACE = 0;
	private static final int L_INFO = 1;
	private static final int L_WARNING = 2;
	private static final int L_ERROR = 3;
	private static final int L_FATAL = 4;
	//private static final int L_NUM_SEVERITIES = 5;
	
	private String LogSeverityNames[] = { "TRACE", "INFO", "WARNING", "ERROR", "FATAL" };
	
	private String m_category;
	private static RhoLogConf m_oLogConf = new RhoLogConf();
	private String m_strMessage;
	private int    m_severity;
	private static String m_SinkLock = "";
	private static IRhoRubyHelper m_sysInfo;
	public static String LOGFILENAME = "RhoLog.txt";
	
	public RhoLogger(String name){
		m_category = name;
	}

	public static RhoLogConf getLogConf(){
		return m_oLogConf;
	}
	
	static
	{
	    EventLogger.register(EVENT_GUID, "RHODESAPP", EventLogger.VIEWER_STRING);
	}
	
	public String getLogCategory(){ return m_category; }
	public void setLogCategory(String category){ m_category = category; }
	
	public static void close(){ RhoLogConf.close(); }
	
	private boolean isEnabled(){
	    if ( m_severity >= getLogConf().getMinSeverity() ){
	        if ( m_category.length() == 0 || m_severity >= L_ERROR )
	            return true;

	        return getLogConf().isCategoryEnabled(m_category);
	    }

	    return false;
	
	}
	
	private String get2FixedDigit(int nDigit){
		if ( nDigit > 9 )
			return Integer.toString(nDigit);
			
		return "0" + Integer.toString(nDigit);
	}
	
	private String getLocalTimeString(){
		Calendar time = Calendar.getInstance();
		String strTime = "";
		strTime += 
			get2FixedDigit(time.get(Calendar.MONTH) + 1) + "/" + 
			get2FixedDigit(time.get(Calendar.DATE)) + "/" +
			time.get(Calendar.YEAR) + " " + 
			get2FixedDigit(time.get(Calendar.HOUR_OF_DAY)) + ":" + 
			get2FixedDigit(time.get(Calendar.MINUTE)) +	":" + 
			get2FixedDigit(time.get(Calendar.SECOND));
			
			//if ( false ) //comment this to show milliseconds
				strTime += ":" + get2FixedDigit(time.get(Calendar.MILLISECOND));
			
		//Date date = time.getTime();
		return strTime;
	}
	
	private String makeStringSize(String str, int nSize)
	{
		if ( str.length() >= nSize )
			return str.substring(0, nSize);
		else {
			String res = "";
			for( int i = 0; i < nSize - str.length(); i++ )
				res += ' ';
			
			res += str;
			
			return res;
		}
	}

	private String makeStringSizeEnd(String str, int nSize)
	{
		if ( str.length() >= nSize )
			return str.substring(str.length()-nSize, str.length());
		else {
			String res = "";
			for( int i = 0; i < nSize - str.length(); i++ )
				res += ' ';
			
			res += str;
			
			return res;
		}
	}
	
	private String getThreadField(){
		String strThread = Thread.currentThread().getName();
		if ( strThread.startsWith("Thread-"))
		{
			try {
				int nThreadID = Integer.parseInt(strThread.substring(7));
				
				return Integer.toHexString(nThreadID);
			}catch(Exception exc){}
		}
		
		return strThread;
	}
	
	private void addPrefix(){
	    //(log level, local date time, thread_id, file basename, line)
	    //I time f5d4fbb0 category|

	    if ( m_severity == L_FATAL )
	    	m_strMessage += LogSeverityNames[m_severity];
	    else
	    	m_strMessage += LogSeverityNames[m_severity].charAt(0);

	    m_strMessage += " " + getLocalTimeString() + ' ' + makeStringSizeEnd(getThreadField(),8) + ' ' +
	    	makeStringSize(m_category,15) + "| ";
	}

	private void logMessage( int severity, String msg ){
		logMessage(severity, msg, null, false );
	}
	private void logMessage( int severity, String msg, Throwable e ){
		logMessage(severity, msg, e, false );
	}
	
	private void logMessage( int severity, String msg, Throwable e, boolean bOutputOnly ){
		m_severity = severity;
		if ( !isEnabled() )
			return;
		
		m_strMessage = "";
	    if ( getLogConf().isLogPrefix() )
	        addPrefix();
		
	    if ( msg != null )
	    	m_strMessage += applyExcludeFilter(msg);
	    
	    if ( e != null )
	    {
	    	m_strMessage += (msg != null && msg.length() > 0 ? ";" : "") + e.getClass().getName() + ": ";
	    	
	    	String emsg = e.getMessage();
	    	if ( emsg != null )
	    		m_strMessage += emsg;
	    }
	    
		if (m_strMessage.length() > 0 || m_strMessage.charAt(m_strMessage.length() - 1) != '\n')
			m_strMessage += '\n';
			
		if ( bOutputOnly )
		{
			System.out.print(m_strMessage);
			if ( e != null && !(e instanceof com.xruby.runtime.lang.RubyException) )
			{
				System.out.print("TRACE: \n");
				e.printStackTrace();
			}
			System.out.flush();
		}else
		{
		    synchronized( m_SinkLock ){
		    	getLogConf().sinkLogMessage( m_strMessage, bOutputOnly );
		    }
		    
		    if ( e != null&& !(e instanceof com.xruby.runtime.lang.RubyException) )
			{
				System.out.print("TRACE: \n");
				e.printStackTrace();
			}
			System.out.flush();		    
		}
	    if ( m_severity == L_FATAL )
	    	processFatalError();
	}

	private String applyExcludeFilter( String strMsg )
	{
	    Vector/*<String>&*/ arSecure = getLogConf().getExcludeAttribs();
	    if ( arSecure.size() == 0 )
	    	return strMsg;
	    
        StringBuffer strRes = new StringBuffer(strMsg.length());
        for ( int i = 0; i < strMsg.length(); i++ )
        {
        	boolean bFound = false;
            for ( int j = 0; j < arSecure.size(); j++ )
            {
                String strExclude = (String)arSecure.elementAt(j);
                if ( strMsg.startsWith( strExclude, i) )
                {	                
                    boolean bSlash = false;
                    int nRemoveStart = i + strExclude.length();
                    
                    int nEndSep = '"';

                    if ( strMsg.startsWith( "\":\"", nRemoveStart) )
                    {
                    	strRes.append(strExclude);
                    	strRes.append("\":\"");
                        nRemoveStart += 3;
                    }
                    else if ( strMsg.startsWith( "\"=>\"", nRemoveStart ) )
                    {
                    	strRes.append(strExclude);
                    	strRes.append("\"=>\"");
                        nRemoveStart += 4;
                    }
                    else if ( strMsg.startsWith( "=", nRemoveStart)  )
                    {
                    	strRes.append(strExclude);
                    	strRes.append("=");
                        nRemoveStart += 1;
                        nEndSep = '&';
                    }
                    else
                        break;

                	
                    int nFill = nRemoveStart;
                    for ( ; nFill < strMsg.length(); nFill++ )
                    {
                        if ( bSlash && strMsg.charAt(nFill) == '\\' ) 
                        {
                            bSlash = false;
                            continue;
                        }
                        else if ( nEndSep != '&' && strMsg.charAt(nFill) == '\\' )
                            bSlash = true;
                        else
                        {
                            if ( strMsg.charAt(nFill) == nEndSep && !bSlash )
                            {
                                //i = nFill;
                                //bFound = true;
                                break;
                            }

                            bSlash = false;
                        }
                    }
                    
                    i = nFill;
                    bFound = true;
                }
                
                if ( bFound )
                    break;
            }
            
            if ( i < strMsg.length() )
            	strRes.append(strMsg.charAt(i));
        }

        return strRes.toString();
	}
	
	static boolean isSimulator()
	{
		return m_sysInfo.isSimulator();
	}
	
	protected void processFatalError(){
    	if ( isSimulator() )
    		throw new RuntimeException();
    	
    	System.exit(0);
	}
	
	public void TRACE(String message) {
		logMessage( L_TRACE, message);
	}
	public void TRACE(String message,Throwable e) {
		logMessage( L_TRACE, message, e );
	}
	
	public void INFO(String message) {
		logMessage( L_INFO, message);
	}

	public void INFO_OUT(String message) {
		logMessage( L_INFO, message, null, true);
	}

	public void INFO_EVENT(String message) 
	{
    	EventLogger.logEvent(EVENT_GUID, (m_category + ": " + message).getBytes());
		
		INFO_OUT(message);
	}
	
	public void WARNING(String message) {
		logMessage( L_WARNING, message);
	}
	public void ERROR(String message) {
		logMessage( L_ERROR, message);
	}
	
	public void ERROR(Throwable e) {
		logMessage( L_ERROR, "", e );
	}
	public void ERROR(String message,Throwable e) {
		logMessage( L_ERROR, message, e );
	}
	public void ERROR_OUT(String message,Throwable e) {
		logMessage( L_ERROR, message, e, true );
	}

	public void ERROR_EVENT(String message,Throwable e) 
	{
		ERROR_OUT(message, e);
		
    	EventLogger.logEvent(EVENT_GUID, m_strMessage.getBytes());
	}
	
	public void FATAL(String message) {
		logMessage( L_FATAL, message);
	}
	public void FATAL(Throwable e) {
		logMessage( L_FATAL, "", e);
	}
	public void FATAL(String message, Throwable e) {
		logMessage( L_FATAL, message, e);
	}
	
	public void ASSERT(boolean exp, String message) {
		if ( !exp )
			logMessage( L_FATAL, message);
	}
	
	public static String getLogText(){
		return m_oLogConf.getLogText();
	}
	
	public static int getLogTextPos(){
		return m_oLogConf.getLogTextPos();
	}
	
	public static void clearLog(){
	    synchronized( m_SinkLock ){
	    	getLogConf().clearLog();
	    }
	}
	
    public static void InitRhoLog()throws Exception
    {
    	m_sysInfo = RhoClassFactory.createRhoRubyHelper();
        RhoConf.InitRhoConf();
        
        //Set defaults
    	m_oLogConf.setLogPrefix(true);		
    	
    	m_oLogConf.setLogToFile(true);
        
		if ( isSimulator() ) {
			m_oLogConf.setMinSeverity( L_INFO );
			m_oLogConf.setLogToOutput(true);
			m_oLogConf.setEnabledCategories("*");
			m_oLogConf.setDisabledCategories("");
	    	m_oLogConf.setMaxLogFileSize(0);//No limit
		}else{
			m_oLogConf.setMinSeverity( L_ERROR );
			m_oLogConf.setLogToOutput(false);
			m_oLogConf.setEnabledCategories("");
	    	m_oLogConf.setMaxLogFileSize(1024*50);
		}
		
    	if ( RhoConf.getInstance().getRhoRootPath().length() > 0 )
	    	m_oLogConf.setLogFilePath( RhoConf.getInstance().getRhoRootPath() + LOGFILENAME );

        //load configuration if exist
    	//
    	//m_oLogConf.saveToFile("");
    	//
    	RhoConf.getInstance().loadConf();
    	m_oLogConf.loadFromConf(RhoConf.getInstance());
    }
    
}
