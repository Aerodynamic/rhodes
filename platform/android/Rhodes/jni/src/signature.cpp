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

#include "rhodes/JNIRhodes.h"

#include "rhodes/jni/com_rhomobile_rhodes_signature_Signature.h"

#include <common/RhodesApp.h>

#undef DEFAULT_LOGCATEGORY
#define DEFAULT_LOGCATEGORY "Signature"

RHO_GLOBAL void JNICALL Java_com_rhomobile_rhodes_signature_Signature_callback
  (JNIEnv *env, jclass, jstring callback, jstring filePath, jstring error, jboolean cancelled)
{
    rho_rhodesapp_callSignatureCallback(rho_cast<std::string>(callback).c_str(),
        rho_cast<std::string>(filePath).c_str(), rho_cast<std::string>(error).c_str(), cancelled);
}


RHO_GLOBAL void rho_signature_take_signature(char* callback_url, char* image_format)
{
///*
    JNIEnv *env = jnienv();
    jclass cls = getJNIClass(RHODES_JAVA_CLASS_SIGNATURE);
    if (!cls) return;
    jmethodID mid = getJNIClassStaticMethod(env, cls, "takeSignature", "(Ljava/lang/String;Ljava/lang/String;)V");
    if (!mid) return;
    jhstring objCallback = rho_cast<jhstring>(callback_url);
    jhstring objFormat = rho_cast<jhstring>(image_format);
    env->CallStaticVoidMethod(cls, mid, objCallback.get(), objFormat.get());
//*/
}

