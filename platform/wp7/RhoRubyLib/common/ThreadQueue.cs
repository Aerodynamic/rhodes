﻿/*------------------------------------------------------------------------
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

using System;

namespace rho.common
{
    public abstract class CThreadQueue : CRhoThread
    {
        private RhoLogger LOG = RhoLogger.RHO_STRIP_LOG ? new RhoEmptyLogger() : 
		    new RhoLogger("ThreadQueue");
	
	    public void setLogCategory(String category)
	    {
            if (category != null)
                LOG.setLogCategory(category);
            else
                LOG = new RhoEmptyLogger();
	    }
	
	    public static int QUEUE_POLL_INTERVAL_SECONDS  = 300;
	    public static int QUEUE_POLL_INTERVAL_INFINITE  = int.MaxValue/1000;
	    public static int QUEUE_STARTUP_INTERVAL_SECONDS  = 10;

        public interface IQueueCommand
        {
            boolean equals(IQueueCommand cmd);
            String toString();
        
            void cancel();
        };

	    private int           m_nPollInterval;
	    private Object m_mxStackCommands;// = new Mutex();
        private LinkedList<IQueueCommand> m_stackCommands = new LinkedList<IQueueCommand>();
   	    private IQueueCommand m_pCurCmd;
   	
        boolean m_bNoThreaded;

        public abstract void processCommand(IQueueCommand pCmd);
        public virtual void onTimeout(){}
    
        public int  getPollInterval(){ return m_nPollInterval;}

        public boolean isNoThreadedMode(){ return m_bNoThreaded; }
        public void setNonThreadedMode(boolean b){m_bNoThreaded = b;}

        public virtual int getLastPollInterval(){ return 0;}
        public virtual boolean isSkipDuplicateCmd() { return false; }

        protected Object getCommandLock(){ return m_mxStackCommands; }
        protected IQueueCommand getCurCommand(){ return m_pCurCmd; }
        protected LinkedList<IQueueCommand> getCommands(){ return m_stackCommands; }
    
        public CThreadQueue()
        {
            m_nPollInterval = QUEUE_POLL_INTERVAL_SECONDS;
            m_bNoThreaded = false;
        
            m_mxStackCommands = getSyncObject();
        }

        protected void addQueueCommandInt(IQueueCommand pCmd)
        {
            LOG.INFO("addCommand: " + pCmd.toString());

            lock(m_mxStackCommands)
            {
	    	    boolean bExist = false;
	            if ( isSkipDuplicateCmd() )
	            {
	    	        for ( int i = 0; i < (int)m_stackCommands.size(); i++ )
	    	        {
	    	    	    if ( m_stackCommands.get(i).equals(pCmd) )
	    		        {
	                        LOG.INFO("Command already exists in queue. Skip it.");
	    			        bExist = true;
	    			        break;
	    		        }
	    	        }
	            }
	
	    	    if ( !bExist )
	    		    m_stackCommands.add(pCmd);
            }
        }

        public void addQueueCommand(IQueueCommand pCmd)
        { 
            addQueueCommandInt(pCmd);

            if ( isNoThreadedMode()  )
                processCommands();
            else if ( isAlive() )
    	        stopWait(); 
        }
    
        public override void stop(int nTimeoutToKill)
        {
            cancelCurrentCommand();
            base.stop(nTimeoutToKill);
        }

        void cancelCurrentCommand()
        {
            lock(m_mxStackCommands)
            {
	            if ( m_pCurCmd != null )
	                m_pCurCmd.cancel();
            }
        }

        protected void processCommandBase(IQueueCommand pCmd)
        {
            lock(m_mxStackCommands)
            {
                m_pCurCmd = pCmd;
            }

            processCommand(pCmd);

            lock(m_mxStackCommands)
            {
                m_pCurCmd = null;
            }
        }
    
        public override void run()
        {
	        LOG.INFO("Starting main routine...");

	        int nLastPollInterval = getLastPollInterval();
	        while( !isStopping() )
	        {
                int nWait = m_nPollInterval > 0 ? m_nPollInterval : QUEUE_POLL_INTERVAL_INFINITE;

                if ( m_nPollInterval > 0 && nLastPollInterval > 0 )
                {
                    int nWait2 = m_nPollInterval - nLastPollInterval;
                    if ( nWait2 <= 0 )
                        nWait = QUEUE_STARTUP_INTERVAL_SECONDS;
                    else
                        nWait = nWait2;
                }

                lock(m_mxStackCommands)
                {
	                if ( nWait >= 0 && !isStopping() && isNoCommands() )
			        {
	                    LOG.INFO("ThreadQueue blocked for " + nWait + " seconds...");
	                    wait(nWait);
	                
	                    if ( isNoCommands() )
	                	    onTimeout();
	                }
                }
                nLastPollInterval = 0;

                if ( !isStopping() )
                {
	        	    try{
	        		    processCommands();
	        	    }catch(Exception e)
	        	    {
	        		    LOG.ERROR("processCommand failed", e);
	        	    }
                }
	        }
	    
	        LOG.INFO("Thread shutdown");	    
        }

        public boolean isNoCommands()
        {
	        boolean bEmpty = false;
	        lock(m_mxStackCommands)
            {		
		        bEmpty = m_stackCommands.isEmpty();
	        }

	        return bEmpty;
        }

        void processCommands()//throws Exception
        {
	        while(!isStopping() && !isNoCommands())
	        {
		        IQueueCommand pCmd = null;
		        lock(m_mxStackCommands)
		        {
    		        pCmd = (IQueueCommand)m_stackCommands.removeFirst();
    	        }
    		
		        processCommandBase(pCmd);
	        }
        }

        public virtual void setPollInterval(int nInterval)
        { 
            m_nPollInterval = nInterval;
            if ( isAlive() )        
        	    stopWait();
        }        
    }
}
