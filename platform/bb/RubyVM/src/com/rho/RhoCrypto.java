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

import net.rim.device.api.crypto.*;
import net.rim.device.api.io.NoCopyByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;
import net.rim.device.api.util.StringUtilities;
import java.io.IOException;

public class RhoCrypto 
{
	AESKey m_key;
	NoCopyByteArrayOutputStream m_outEncrypt;
	BlockEncryptor m_cryptoStream;
	AESDecryptorEngine m_decryptEngine;
	String m_strStorageKey;
	int m_nAlgLen;
	
	public RhoCrypto(String strStorageKey, int nAlgLen )
	{
		m_strStorageKey = strStorageKey;
		m_nAlgLen = nAlgLen;
	}
	
	public void close()
	{
		try
		{
			if ( m_cryptoStream != null )
				m_cryptoStream.close();
			
			if ( m_outEncrypt  != null )
				m_outEncrypt.close();
		}catch(IOException exc)
		{
			
		}
	}
	
	public static boolean isKeyExist(String strStorageKey)
	{
		long lKey = makeKeyHash(strStorageKey);
		PersistentObject persInfo = PersistentStore.getPersistentObject(lKey);
		byte[] keyData = (byte[])persInfo.getContents();
		return keyData != null;		
	}

	private static long makeKeyHash(String strStorageKey)
	{
		String strInfoKey = strStorageKey + "rho_encrypt_key_706cf99e52031270f635657f0272b8b516a5ee721";
		long lHash = StringUtilities.stringHashToLong(strInfoKey);
		
		//System.out.println("makeKeyHash: " + strInfoKey + ":" + lHash);
		return lHash;
	}
	
	private void init()throws Exception
	{
		if ( m_key != null )
			return;
		
		long lKey = makeKeyHash(m_strStorageKey);
		
		PersistentObject persInfo = PersistentStore.getPersistentObject(lKey);
		byte[] keyData = (byte[])persInfo.getContents();
		if ( keyData == null )
		{
			m_key = new AESKey(m_nAlgLen);
			keyData = m_key.getData();
			persInfo.setContents(keyData);
			persInfo.commit();
		}else
		{
			m_key = new AESKey(keyData, 0, m_nAlgLen);
		}
		
		m_outEncrypt = new NoCopyByteArrayOutputStream();
		m_cryptoStream = new BlockEncryptor( new AESEncryptorEngine( m_key ), m_outEncrypt );
		
		m_decryptEngine = new AESDecryptorEngine( m_key );
	}
	
	public void encrypt(byte[] plainText, int offset, int dataLength, byte[] cipherText, int outOffset )throws Exception
	{
		init();
		
    	m_outEncrypt.reset();
        m_cryptoStream.write( plainText, offset, dataLength );
        m_cryptoStream.flush();
        
        int finalLength = m_outEncrypt.size();
        System.arraycopy( m_outEncrypt.getByteArray(), 0, cipherText, outOffset, finalLength );	        
	}
	
	public void decrypt(byte[] cipherText, int offset, int dataLength, byte[] plainText, int outOffset)throws Exception
	{
		init();
        // Create the input stream based on the ciphertext
        ByteArrayInputStream in = new ByteArrayInputStream( cipherText, offset, dataLength );
        
        BlockDecryptor cryptoStream = null;
        try
        {
        	cryptoStream = new BlockDecryptor( m_decryptEngine, in );
	        cryptoStream.read( plainText, outOffset, dataLength );
        }finally
        {
        	if ( cryptoStream != null )
        		cryptoStream.close();
        	
        	if ( in != null )
        		in.close();        	
        }
	}	
}
