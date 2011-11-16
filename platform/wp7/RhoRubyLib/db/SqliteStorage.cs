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
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Community.CsharpSqlite;
using System.Globalization;
using IronRuby.Runtime;
using IronRuby.Builtins;
using rho.common;

namespace rho.db
{
    public class CSqliteStorage : IDBStorage
    {
        private static RhoLogger LOG = RhoLogger.RHO_STRIP_LOG ? new RhoEmptyLogger() :
            new RhoLogger("SqliteStorage");

        private boolean m_bPendingTransaction = false;
        private Sqlite3.sqlite3 m_db;
        private int m_nInsideTransaction = 0;
        private boolean m_bNeedProcessCallback = false;
        private IDBCallback m_dbCallback;

        public void close()
        {
            lock (m_db)
            {
                if (m_db != null)
                {
                    int rc = Sqlite3.sqlite3_close(m_db);
                    checkError(rc);
                    m_db = null;
                }
            }
        }

        public void commit()
        {
            lock (m_db)
            {
                if (m_nInsideTransaction > 0)
                    m_nInsideTransaction--;

                if (m_db != null)
                {
                    Sqlite3.sqlite3_exec(m_db, "END;", 0, 0, 0);
                    checkError();
                }
            }
        }

        public IDBResult createResult()
        {
            return new CSqliteResult();
        }

        public void createTriggers()
        {
            String strTriggers = CRhoFile.readStringFromResourceFile("db/syncdb.triggers");
            executeBatchSQL(strTriggers);
        }

        public void deleteAllFiles(String strPath)
        {
            CRhoFile.deleteFile(strPath);
            CRhoFile.deleteFile(strPath + "-journal");
        }

        public void executeBatchSQL(String strStatement)
        {
            lock (m_db)
            {
                Sqlite3.sqlite3_exec(m_db, strStatement, 0, 0, 0);
                checkError();
            }
        }

        void checkError()
        {
            int res = Sqlite3.sqlite3_errcode(m_db);
            checkError(res);
        }

        void checkError(int res)
        {
            if (res != Sqlite3.SQLITE_OK)
                throw new DBException(res, Sqlite3.sqlite3_errmsg(m_db));
        }

        public IDBResult executeSQL(String strStatement, Object[] values, boolean bReportNonUnique, boolean bNoCopy)
        {
            if (m_db == null)
                throw new Exception("executeSQL: m_db == null");

            lock (m_db)
            {
                var stmt = Prepare(strStatement, values);
                CSqliteResult res = new CSqliteResult(stmt, bNoCopy);
                int rc = res.executeStatement();
                if (rc != Sqlite3.SQLITE_ROW)
                {
                    if (bReportNonUnique && rc == Sqlite3.SQLITE_CONSTRAINT)
                    {
                        res.close();
                        return new CSqliteResult(true);
                    }

                    res.checkError();
                    res.close();
                }
                return res;
            }

        }

        public string[] getAllTableNames()
        {
            IDBResult res = executeSQL("SELECT name FROM sqlite_master WHERE type='table'", null, false, false);

            Vector<Object> arTables = new Vector<Object>();
            for (; !res.isEnd(); res.next())
            {
                arTables.addElement(res.getCurData()[0]);
            }

            String[] vecTables = new String[arTables.size()];
            for (int i = 0; i < arTables.size(); i++)
                vecTables[i] = (String)arTables.elementAt(i);

            return vecTables;
        }

        public boolean isDbFileExists(string strPath)
        {
            return CRhoFile.isFileExist(strPath);
        }
        
        public boolean isTableExists(string strName)
        {
            Object[] vals = { strName };
		    IDBResult res = executeSQL("SELECT name FROM sqlite_master WHERE type='table' AND name=?", vals, false, false );
		    boolean bRes = !res.isEnd();
		    res.close();
		
		    return bRes;
        }

        public void onBeforeCommit()
        {
            if (!m_bNeedProcessCallback)
                return;
            processCallbackData();
        }

        public void open(String strPath, String strSqlScript, String strEncryptionInfo)
        {
            try
            {
                String dbURI = RHODESAPP().resolveDBFilesPath(strPath);
                dbURI = CFilePath.removeFirstSlash(dbURI);
                boolean bEncrypted = strEncryptionInfo != null && strEncryptionInfo.length() > 0;
                //DatabaseSecurityOptions dbso = new DatabaseSecurityOptions(bEncrypted);

                boolean bExist = CRhoFile.isFileExist(strPath);

                int res = Sqlite3.sqlite3_open(dbURI, ref m_db);
                if (res != Sqlite3.SQLITE_OK)
                    throw new DBException(res, "Could not open database file: " + strPath);

                res = Sqlite3.sqlite3_exec(m_db, "PRAGMA journal_mode=PERSIST", 0, 0, 0);
                if (res != Sqlite3.SQLITE_OK)
                {
                    Sqlite3.sqlite3_close(m_db);
                    m_db = null;
                    throw new DBException(res, "Cannot set journal mode to PERSIST: " + strPath);
                }

                string[] ar1 = CRhoFile.enumDirectory("db");

                if (!bExist)
                    createSchema(strSqlScript);

                Sqlite3.sqlite3_create_function(m_db, "rhoOnDeleteObjectRecord", 3, Sqlite3.SQLITE_ANY, 0,
                                                DBAdapter.SyncBlob_DeleteCallback, null, null);
                Sqlite3.sqlite3_create_function(m_db, "rhoOnUpdateObjectRecord", 3, Sqlite3.SQLITE_ANY, 0,
                                                DBAdapter.SyncBlob_UpdateCallback, null, null);

                string[] ar2 = CRhoFile.enumDirectory("db");

                if (m_bPendingTransaction)
                    startTransaction();

                m_bPendingTransaction = false;
            }
            catch (Exception exc)
            {
                throw new DBException(exc);
            }
            finally
            {
                m_nInsideTransaction = 0;
            }
        }

        public void rollback()
        {
            if (m_nInsideTransaction > 0)
                m_nInsideTransaction--;

            lock (m_db)
            {
                if (m_db != null)
                {
                    Sqlite3.sqlite3_exec(m_db, "ROLLBACK;", 0, 0, 0);
                    checkError();
                }
            }
        }

        public void setDbCallback(IDBCallback callback)
        {
            m_dbCallback = callback;
        }

        public void startTransaction()
        {
            lock (m_db)
            {
                if (m_db == null)
                    m_bPendingTransaction = true;
                else
                {
                    Sqlite3.sqlite3_exec(m_db, "BEGIN IMMEDIATE;", 0, 0, 0);
                    checkError();
                }

                m_nInsideTransaction++;
                m_bNeedProcessCallback = false;
            }
        }

        #region Helpers

        private void createSchema(String strSqlScript)
        {
            if ( strSqlScript.length() == 0 )
            {
                LOG.ERROR("createSchema failed. Cannot read schema: " + strSqlScript);
                return;
            }

            executeBatchSQL(strSqlScript);
            createTriggers();
        }

        private void Bind(Sqlite3.Vdbe stmt, Object[] values)
        {
            for (int i = 0; values != null && i < values.Length; i++)
            {
                var obj = values[i];
                if (obj == null)
                    Sqlite3.sqlite3_bind_null(stmt, i + 1);
                else if (obj is Byte || obj is UInt16 || obj is SByte || obj is Int16 || obj is Int32)
                    Sqlite3.sqlite3_bind_int(stmt, i + 1, Convert.ToInt32(obj, CultureInfo.InvariantCulture));
                else if (obj is UInt32 || obj is Int64)
                    Sqlite3.sqlite3_bind_int64(stmt, i + 1, Convert.ToInt64(obj, CultureInfo.InvariantCulture));
                else if (obj is Single || obj is Double || obj is Decimal)
                    Sqlite3.sqlite3_bind_double(stmt, i + 1, Convert.ToDouble(obj, CultureInfo.InvariantCulture));
                else if (obj is String)
                    Sqlite3.sqlite3_bind_text(stmt, i + 1, (String)obj, ((String)obj).Length, null);
                else if (obj is MutableString)
                    Sqlite3.sqlite3_bind_text(stmt, i + 1, obj.ToString(), ((MutableString)obj).Length, null);
                else if (obj is byte[])
                    Sqlite3.sqlite3_bind_blob(stmt, i + 1, (byte[])obj, ((byte[])obj).Length, null);
                else if (obj is RubySymbol)
                {
                    String val = ((RubySymbol)obj).ToString();
                    Sqlite3.sqlite3_bind_text(stmt, i + 1, val, val.Length, null);
                }
                else if (obj is Boolean)
                {
                    String val = ((Boolean)obj) ? "true" : "false";
                    Sqlite3.sqlite3_bind_text(stmt, i + 1, val, val.Length, null);
                }
                else
                {
                    String val = obj.ToString();
                    Sqlite3.sqlite3_bind_text(stmt, i + 1, val, val.Length, null);
                }

                checkError();
            }
        }

        private Sqlite3.Vdbe Prepare(String strStatement, Object[] values)
        {
            Sqlite3.Vdbe ppStmt=new Sqlite3.Vdbe();
            Sqlite3.sqlite3_prepare_v2(m_db, strStatement, strStatement.Length, ref ppStmt, 0);
            checkError();
            
            Bind(ppStmt, values);
            return ppStmt;
        }

        private void processCallbackData()
    	{
        }

        private static CRhodesApp RHODESAPP() { return CRhodesApp.Instance; }

        #endregion
    }
}
