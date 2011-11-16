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

package rhomobile.camera;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import com.rho.RhoEmptyLogger;
import com.rho.RhoLogger;

import net.rim.device.api.io.file.FileSystemJournal;
import net.rim.device.api.io.file.FileSystemJournalEntry;
import net.rim.device.api.io.file.FileSystemJournalListener;

public class CameraFilesListener implements FileSystemJournalListener {
	
	private static final RhoLogger LOG = RhoLogger.RHO_STRIP_LOG ? new RhoEmptyLogger() : 
		new RhoLogger("CameraFilesListener");
	
	private long _lastUSN = 0;
	
	private CameraScreen _screen;
	
	private Hashtable _exists;
	
	private final String[] _folders = {
			"file:///store/home/user/pictures/",
			"file:///sdcard/blackberry/pictures/",
			"file:///store/home/user/camera/",
			"file:///sdcard/blackberry/camera/"			
	};
	
	public CameraFilesListener(CameraScreen screen) {
		_screen = screen;
		_exists = new Hashtable();
		
		for(int i = 0; i < _folders.length; i++) {
			try {
				FileConnection fconn = (FileConnection)Connector.open(_folders[i], Connector.READ);
				if (!fconn.isDirectory())
					continue;
				Enumeration e = fconn.list();
				while(e.hasMoreElements()) {
					_exists.put(e.nextElement(), new Boolean(true));
				}
			} catch (IOException e) {
				LOG.ERROR("CameraFilesListener: " + _folders[i], e);
				continue;
			}
		}
	}

	public void fileJournalChanged() {
		long nextUSN = FileSystemJournal.getNextUSN();
        String msg = null;
        for (long lookUSN = nextUSN - 1; lookUSN >= _lastUSN && msg == null; --lookUSN) {
            FileSystemJournalEntry entry = FileSystemJournal.getEntry(lookUSN);
            if (entry == null) { // we didn't find an entry.
                break;
            }

            //check if this entry was added or deleted
            String path = entry.getPath();
            if (path == null)
            	continue;
            if (!path.startsWith("file://"))
            	path = "file://" + path;
            
        	int event = entry.getEvent();
        	LOG.TRACE("event: " + Integer.toString(event));
        	if (event != FileSystemJournalEntry.FILE_ADDED)
        		continue;
        	
        	String lpath = path.toLowerCase();
        	boolean bKnownFolder = false;
        	for( int i = 0; i < _folders.length; i++)
        	{
        		if ( lpath.startsWith(_folders[i]))
        		{
        			bKnownFolder = true;
        			break;
        		}
        	}
        	if (!bKnownFolder)
        		continue;
        	
        	if (lpath.endsWith(".dat"))
        		continue;
        	
        	if (_exists.get(path) != null)
    			continue;
        	
        	if (_screen != null) {
				try {
					FileConnection fconn = (FileConnection)Connector.open(path, Connector.READ);
					boolean exists = fconn.exists();
					fconn.close();
					if (!exists)
	        			continue;
				} catch (IOException e) {
					continue;
				}
				_exists.put(path, new Boolean(true));
        		_screen.invokeCloseScreen(true, path);
        		_screen = null;
        	}
        }
        
        _lastUSN = nextUSN;
	}

}
