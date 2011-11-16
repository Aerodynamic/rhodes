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

import com.rho.db.*;
import com.rho.net.*;
import com.rho.file.*;
import java.io.IOException;

public class RhoClassFactory 
{ 
	private static final RhoLogger LOG = RhoLogger.RHO_STRIP_LOG ? new RhoEmptyLogger() : 
		new RhoLogger("ClassFactory");
	
    public static SimpleFile createFile() throws Exception 
    {
        Class wrapperClass;
        try {
            wrapperClass = Class.forName("com.rho.file.Jsr75File");
        } catch (ClassNotFoundException exc) {  
/*        	try {
                wrapperClass = Class.forName("com.rhomobile.rhodes.AndroidFile"); //android
            } catch (ClassNotFoundException e) {
	        	LOG.ERROR_OUT("createFile - Class not found",e);    	

                throw e;
            }*/
        	throw exc;
        }
        
        try {
        	return (SimpleFile)wrapperClass.newInstance();
        }catch(Exception e)
        {
        	LOG.ERROR_OUT("createFile - newInstance failed",e);    	
        	
        	throw e;
        }
        
    }
    
    public static IFileAccess createFileAccess() throws Exception
    {
    	return RhoClassFactory.createRhoRubyHelper().createFileAccess();
    }
    
    public static IRAFile createRAFile() throws Exception
    {
    	return RhoClassFactory.createRhoRubyHelper().createRAFile();
    }

    public static IRAFile createFSRAFile() throws Exception
    {
    	return RhoClassFactory.createRhoRubyHelper().createFSRAFile();
    }

    public static IDBStorage createDBStorage() throws Exception
    {
        Class wrapperClass;
        try {
        	if (Capabilities.USE_SQLITE)
        		wrapperClass = Class.forName("com.rho.db.SqliteStorage");
        	else
        		wrapperClass = Class.forName("com.rho.db.HsqlDBStorage");
        } catch (ClassNotFoundException exc) {  
        	throw exc;
        }
    	
        try {
        	return (IDBStorage)wrapperClass.newInstance();
        }catch(Exception e)
        {
        	LOG.ERROR_OUT("createDBStorage - newInstance failed",e);    	
        	
        	throw e;
        }
    }

    public static IRhoRubyHelper createRhoRubyHelper() throws Exception
    {
    	LOG.TRACE("createRhoRubyHelper");    	
    	
        Class wrapperClass;
        try {
            wrapperClass = Class.forName("com.rho.RhoRubyHelper"); //bb
        } catch (ClassNotFoundException exc) {  
        	/*try {
                wrapperClass = Class.forName("com.rhomobile.rhodes.RhoRubyHelper"); //android
            } catch (ClassNotFoundException e) {
	        	LOG.ERROR("createRhoRubyHelper- Class not found",e);    	
            	
                throw e;
            }*/
        	throw exc;
        }
        
        try{
        	return (IRhoRubyHelper)wrapperClass.newInstance();
        }catch(Exception e)
        {
        	LOG.ERROR("createRhoRubyHelper - newInstance failed",e);    	
        	
        	throw e;
        }
        	
    }

    static INetworkAccess m_NAInstance;
    public static INetworkAccess getNetworkAccess() throws IOException
    {
    	try{
	    	if ( m_NAInstance == null )
	    	{    	
	        	LOG.TRACE("getNetworkAccess");    	
	    		
		        Class wrapperClass;
		        try {
		            wrapperClass = Class.forName("com.rho.net.NetworkAccess");
		        } catch (ClassNotFoundException exc) {  
		        	/*try {
			            wrapperClass = Class.forName("com.rhomobile.rhodes.NetworkAccessImpl"); //android 
			        } catch (ClassNotFoundException e) {
			        	LOG.ERROR("getNetworkAccess- Class not found",e);    	
			        	
			            throw e;
			        }*/
		        	throw exc;
		        }
		        
		        m_NAInstance = (INetworkAccess)wrapperClass.newInstance();
	    	}
    	}catch(Exception exc){
        	LOG.ERROR("getNetworkAccess - newInstance failed",exc);    	
    		
	    	throw new IOException(exc.getMessage());
	    }
	    return m_NAInstance;
    }
    
    public static NetRequest createNetRequest()
    {
    	return new NetRequest();
    }
}
