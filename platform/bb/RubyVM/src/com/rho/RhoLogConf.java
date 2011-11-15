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

import java.io.IOException;
import com.rho.file.SimpleFile;
import java.util.Vector;

public class RhoLogConf {
    int         m_nMinSeverity = 0;
    boolean     m_bLogToOutput = false;

    boolean     m_bLogToFile = false;
    String      m_strLogFilePath = "";
    int         m_nMaxLogFileSize = 0;

    boolean     m_bLogPrefix = false;

    String      m_strEnabledCategories = "", m_strDisabledCategories = "";
    Vector/*<String>*/  m_arExcludeAttribs = new Vector(); 
    
    IRhoLogSink   m_pFileSink = null;
    IRhoLogSink   m_pOutputSink = null;

	public RhoLogConf(){
		m_pFileSink = new RhoLogFileSink(this);
		m_pOutputSink = new RhoLogOutputSink(this);
    }

	public static void close(){ 
		//TODO: should we close log file? some threads may still logging 
	}
	
	RhoConf RHOCONF(){ return RhoConf.getInstance(); }
	
    public void saveToFile()
    {
        RHOCONF().setInt("MinSeverity", getMinSeverity(), true );
        RHOCONF().setBool("LogToOutput", isLogToOutput(), true );
        RHOCONF().setBool("LogToFile", isLogToFile(), true  );
        RHOCONF().setString("LogFilePath", getLogFilePath(), true  );
        RHOCONF().setInt("MaxLogFileSize", getMaxLogFileSize(), true  );
        RHOCONF().setString("LogCategories", getEnabledCategories(), true  );
        RHOCONF().setString("ExcludeLogCategories", getDisabledCategories(), true  );
    }
    
    void loadFromConf(RhoConf oRhoConf){
        if ( oRhoConf.isExist( "MinSeverity" ) )
            setMinSeverity( oRhoConf.getInt("MinSeverity") );
        if ( oRhoConf.isExist( "LogToOutput") )
            setLogToOutput( oRhoConf.getBool("LogToOutput") );
        if ( oRhoConf.isExist( "LogToFile") )
            setLogToFile( oRhoConf.getBool("LogToFile"));
        if ( oRhoConf.isExist( "LogFilePath") )
            setLogFilePath( oRhoConf.getString("LogFilePath") );
        if ( oRhoConf.isExist( "MaxLogFileSize") )
            setMaxLogFileSize( oRhoConf.getInt("MaxLogFileSize") );
        if ( oRhoConf.isExist( "LogCategories") )
            setEnabledCategories( oRhoConf.getString("LogCategories") );
        if (oRhoConf.isExist( "ExcludeLogCategories") )
            setDisabledCategories( oRhoConf.getString("ExcludeLogCategories") );
        if ( oRhoConf.isExist( "log_exclude_filter") )
        	setExcludeFilter( oRhoConf.getString("log_exclude_filter") );        
    }
	
    public int getMinSeverity(){ return m_nMinSeverity; }
    public void setMinSeverity(int nMinSeverity){ m_nMinSeverity = nMinSeverity; }

    boolean isLogToOutput(){ return m_bLogToOutput;}
    void setLogToOutput(boolean bLogToOutput){ m_bLogToOutput = bLogToOutput;}

    boolean isLogToFile(){ return m_bLogToFile;}
    void setLogToFile(boolean bLogToFile){ m_bLogToFile = bLogToFile;}

    String getLogFilePath(){ return m_strLogFilePath;}
    void setLogFilePath(String szLogFilePath)
    {
        if ( !m_strLogFilePath.equals(szLogFilePath) ){
            m_strLogFilePath = szLogFilePath; 

            if ( m_pFileSink != null){
            	m_pFileSink.close();
            }
        }
    }

	public void clearLog(){
        if ( m_pFileSink != null){
        	m_pFileSink.clear();
        }
	}
    
    void setMaxLogFileSize(int nMaxSize){m_nMaxLogFileSize = nMaxSize; }
    int getMaxLogFileSize(){ return m_nMaxLogFileSize; }

    boolean isLogPrefix(){ return m_bLogPrefix;}
    void setLogPrefix(boolean bLogPrefix){ m_bLogPrefix = bLogPrefix;}

    public synchronized void setEnabledCategories( String szCatList ){m_strEnabledCategories = szCatList; }
    public synchronized void setDisabledCategories( String szCatList ){ m_strDisabledCategories = szCatList; }
    
    public String getEnabledCategories(){ return m_strEnabledCategories; }
    public String getDisabledCategories(){ return m_strDisabledCategories; }
    
    synchronized boolean isCategoryEnabled(String cat){
        //TODO: Optimize categories search : add map

        if ( m_strDisabledCategories.indexOf(cat) >= 0 )
            return false;

        if ( m_strEnabledCategories.length() == 0 )
            return false;

        return m_strEnabledCategories.equals("*") || m_strEnabledCategories.indexOf(cat) >= 0;
    }

    Vector/*<String>&*/ getExcludeAttribs(){ return m_arExcludeAttribs; }    
    
    void setExcludeFilter( String strExcludeFilter )
    {
        if ( strExcludeFilter != null && strExcludeFilter.length() > 0 )
        {
            com.rho.Tokenizer oTokenizer = new com.rho.Tokenizer( strExcludeFilter, "," );
    	    while (oTokenizer.hasMoreTokens()) 
            {
                String tok = oTokenizer.nextToken().trim();
    		    if (tok.length() == 0)
    			    continue;

                //m_arExcludeAttribs.addElement( "\"" + tok + "\"=>\"" );
    		    m_arExcludeAttribs.addElement( tok );
            }    	
        }
        else
        	m_arExcludeAttribs.removeAllElements();
    }
    
    void sinkLogMessage( String strMsg, boolean bOutputOnly ){
        if ( !bOutputOnly && isLogToFile() )
            m_pFileSink.writeLogMessage(strMsg);

        //Should be at the end
        if ( isLogToOutput() )
            m_pOutputSink.writeLogMessage(strMsg);
    	
    }
    
    int  getLogTextPos(){
        return m_pFileSink != null ? m_pFileSink.getCurPos() : -1;
    }
    
	public String getLogText(){
		String res = "";
    	SimpleFile oFile = null;
	    boolean bOldSaveToFile = isLogToFile();
	    setLogToFile(false);
    	
    	try{
	        oFile = RhoClassFactory.createFile();
	        oFile.open( getLogFilePath(), true, false);
	        
	        if ( oFile.isOpened() ){
	            res = oFile.readString();
	            oFile.close();
	        }
	        
    	}catch(Exception exc){
    		if ( oFile != null )
    			try{ oFile.close(); }catch(IOException exc2){}
    	}finally
    	{
    		setLogToFile(bOldSaveToFile);
    	}
		
		return res;
	}

}
