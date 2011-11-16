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
using System.Collections.Generic;
using System.ComponentModel;
using System.Net.Sockets;
using System.Net;
using System.Threading;
using System.Text;
using System.IO;

namespace RhoLogServer
{
    public partial class SocketServer
    {
        const int MAX_CLIENTS = 10;
        const int PORT = 8000;
        String m_logPath;

        private Socket m_mainSocket;
        private List<StateObject> m_workerSocket = new List<StateObject>();



        public SocketServer(String logPath)
        {
            m_logPath = logPath;
            start_listening();
        }

        private void start_listening()
        {
            try
            {

                if (m_logPath != null)
                    File.Delete(m_logPath);
                // Create the listening socket...
                m_mainSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
                IPEndPoint ipLocal = new IPEndPoint(IPAddress.Any, PORT);
                // Bind to local IP Address...
                m_mainSocket.Bind(ipLocal);
                // Start listening...
                m_mainSocket.Listen(1000);
                // Create the call back for any client connections...
                m_mainSocket.BeginAccept(new AsyncCallback(OnClientConnect), null);
                // m_mainSocket.BeginAccept(new AsyncCallback(AcceptCallback), m_mainSocket);

            }
            catch (Exception se)
            {
                Environment.Exit(0);
            }

        }


        // This is the call back function, which will be invoked when a client is connected
        public void OnClientConnect(IAsyncResult asyn)
        {
            try
            {
                // Here we complete/end the BeginAccept() asynchronous call
                // by calling EndAccept() - which returns the reference to
                // a new Socket object

                Socket handler = m_mainSocket.EndAccept(asyn);
                // Let the worker Socket do the further processing for the 
                // just connected client
                StateObject state = new StateObject();
                state.socket = handler;
                state.id = m_workerSocket.Count;

                m_workerSocket.Add(state);
                try
                {
                    handler.BeginReceive(state.buffer, 0, StateObject.BufferSize, 0, new AsyncCallback(ReadCallback), state);
                }
                catch (SocketException se)
                {
                    //MessageBox.Show(se.Message);
                }

                // Display this client connection as a status message on the GUI	
                String str = String.Format("Client # {0} connected", m_workerSocket.Count - 1);

                // Since the main Socket is now free, it can go back and wait for
                // other clients who are attempting to connect
                m_mainSocket.BeginAccept(new AsyncCallback(OnClientConnect), null);

            }
            catch (ObjectDisposedException)
            {
                
            }
            catch (SocketException se)
            {
                
            }
        }


        protected void ReadCallback(IAsyncResult ar)
        {
            String content = String.Empty;
            // Retrieve the state object and the handler socket
            // from the async state object.
            StateObject state = (StateObject)ar.AsyncState;
            Socket handler = state.socket;
            int id = state.id;
            try
            {
                // Read data from the client socket.
                int bytesRead = handler.EndReceive(ar);

                if (bytesRead > 0)
                {
                    // There might be more data, so store the data received so far.
                    state.sb.Append(Encoding.UTF8.GetString(state.buffer, 0, bytesRead));
                    content = state.sb.ToString();
                    if ((content.Length > 0) || (content.IndexOf("") > -1))
                    {
                        Object objData = content;
                        byte[] byData = System.Text.Encoding.UTF8.GetBytes(objData.ToString());
                        FileStream st = File.Open(m_logPath, FileMode.OpenOrCreate | FileMode.Append);
                        st.Write(byData, 0, byData.Length);
                        st.Close();
                    }
                    //handler.BeginReceive(state.buffer, 0, StateObject.BufferSize, 0, new AsyncCallback(this.ReadCallback), state);
                    //closeSocket(state.id);
                    closeSocket(state.id);
                }
                else
                {
                    closeSocket(state.id);
                }
            }
            catch (System.Net.Sockets.SocketException es)
            {
                closeSocket(state.id);
                if (es.ErrorCode != 64)
                {
                   
                }
            }
            catch (Exception e)
            {
                closeSocket(state.id);
                if (e.GetType().FullName != "System.ObjectDisposedException")
                {
                    
                }
            }
        }




        private void closeSocket(int id)
        {
            String str = String.Format("Client # {0} disconnected", m_workerSocket[id].id);
            m_workerSocket[id].socket.Close();
            m_workerSocket[id] = null;
        }

        private String GetIP()
        {
            String strHostName = Dns.GetHostName();

            // Find host by name
            IPHostEntry iphostentry = Dns.GetHostEntry(strHostName);

            // Grab the first IP addresses
            String IPStr = "";
            foreach (IPAddress ipaddress in iphostentry.AddressList)
            {
                IPStr = ipaddress.ToString();
                return IPStr;
            }
            return IPStr;
        }

        private void CloseSockets()
        {

            if (m_mainSocket != null)
            {
                m_mainSocket.Close();
            }
            for (int i = 0; i < m_workerSocket.Count; i++)
            {
                if (m_workerSocket[i] != null)
                {
                    m_workerSocket[i].socket.Close();
                    m_workerSocket[i] = null;
                }
            }
        }


        public class StateObject
        {
            public Socket socket = null;	// Client socket.
            public const int BufferSize = 65000;	// Size of receive buffer.
            public byte[] buffer = new byte[BufferSize];// Receive buffer.
            public StringBuilder sb = new StringBuilder();//Received data String.
            public int id = -1; // client id.
        }


    }
}
