/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2002,2003,2004,2005,2006 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch;
import java.net.Socket;

class Util{

  private static final byte[] b64 ="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".getBytes();
  private static byte val(byte foo){
    if(foo == '=') return 0;
    for(int j=0; j<b64.length; j++){
      if(foo==b64[j]) return (byte)j;
    }
    return 0;
  }
  static byte[] fromBase64(byte[] buf, int start, int length){
    byte[] foo=new byte[length];
    int j=0;
    for (int i=start;i<start+length;i+=4){
      foo[j]=(byte)((val(buf[i])<<2)|((val(buf[i+1])&0x30)>>>4));
      if(buf[i+2]==(byte)'='){ j++; break;}
      foo[j+1]=(byte)(((val(buf[i+1])&0x0f)<<4)|((val(buf[i+2])&0x3c)>>>2));
      if(buf[i+3]==(byte)'='){ j+=2; break;}
      foo[j+2]=(byte)(((val(buf[i+2])&0x03)<<6)|(val(buf[i+3])&0x3f));
      j+=3;
    }
    byte[] bar=new byte[j];
    System.arraycopy(foo, 0, bar, 0, j);
    return bar;
  }
  static byte[] toBase64(byte[] buf, int start, int length){

    byte[] tmp=new byte[length*2];
    int i,j,k;
    
    int foo=(length/3)*3+start;
    i=0;
    for(j=start; j<foo; j+=3){
      k=(buf[j]>>>2)&0x3f;
      tmp[i++]=b64[k];
      k=(buf[j]&0x03)<<4|(buf[j+1]>>>4)&0x0f;
      tmp[i++]=b64[k];
      k=(buf[j+1]&0x0f)<<2|(buf[j+2]>>>6)&0x03;
      tmp[i++]=b64[k];
      k=buf[j+2]&0x3f;
      tmp[i++]=b64[k];
    }

    foo=(start+length)-foo;
    if(foo==1){
      k=(buf[j]>>>2)&0x3f;
      tmp[i++]=b64[k];
      k=((buf[j]&0x03)<<4)&0x3f;
      tmp[i++]=b64[k];
      tmp[i++]=(byte)'=';
      tmp[i++]=(byte)'=';
    }
    else if(foo==2){
      k=(buf[j]>>>2)&0x3f;
      tmp[i++]=b64[k];
      k=(buf[j]&0x03)<<4|(buf[j+1]>>>4)&0x0f;
      tmp[i++]=b64[k];
      k=((buf[j+1]&0x0f)<<2)&0x3f;
      tmp[i++]=b64[k];
      tmp[i++]=(byte)'=';
    }
    byte[] bar=new byte[i];
    System.arraycopy(tmp, 0, bar, 0, i);
    return bar;

//    return sun.misc.BASE64Encoder().encode(buf);
  }

  static String[] split(String foo, String split){
    if(foo==null)
      return null;
    byte[] buf=foo.getBytes();
    java.util.Vector bar=new java.util.Vector();
    int start=0;
    int index;
    while(true){
      index=foo.indexOf(split, start);
      if(index>=0){
	bar.addElement(new String(buf, start, index-start));
	start=index+1;
	continue;
      }
      bar.addElement(new String(buf, start, buf.length-start));
      break;
    }
    String[] result=new String[bar.size()];
    for(int i=0; i<result.length; i++){
      result[i]=(String)(bar.elementAt(i));
    }
    return result;
  }
  static boolean glob(byte[] pattern, byte[] name){
    return glob0(pattern, 0, name, 0);
  }
  static private boolean glob0(byte[] pattern, int pattern_index,
			      byte[] name, int name_index){
    if(name.length>0 && name[0]=='.'){
      if(pattern.length>0 && pattern[0]=='.'){
        if(pattern.length==2 && pattern[1]=='*') return true;
        return glob(pattern, pattern_index+1, name, name_index+1);
      }
      return false;
    }
    return glob(pattern, pattern_index, name, name_index);
  }
  static private boolean glob(byte[] pattern, int pattern_index,
			      byte[] name, int name_index){
//System.err.println("glob: "+new String(pattern)+", "+new String(name));
    int patternlen=pattern.length;
    if(patternlen==0)
      return false;
    int namelen=name.length;
    int i=pattern_index;
    int j=name_index;
    while(i<patternlen && j<namelen){
//System.err.println("i="+i+", j="+j);
      if(pattern[i]=='\\'){
	if(i+1==patternlen)
	  return false;
	i++;
	if(pattern[i]!=name[j]) return false;
	i++; j++;
	continue;
      }
      if(pattern[i]=='*'){
	if(patternlen==i+1) return true;
	i++;
	byte foo=pattern[i];
	while(j<namelen){
	  if(foo==name[j]){
	    if(glob(pattern, i, name, j)){
	      return true;
	    }
	  }
	  j++;
	}
	return false;
	/*
	if(j==namelen) return false;
	i++; j++;
	continue;
	*/
      }
      if(pattern[i]=='?'){
	i++; j++;
	continue;
      }
      if(pattern[i]!=name[j]) return false;
      i++; j++;
      if(!(j<namelen)){
        if(!(i<patternlen)){
	  return true;
	}
	if(pattern[i]=='*' 
           //&& !((i+1)<patternlen)
	   ){
	  return true;
	}
      }
      continue;
    }
    if(i==patternlen && j==namelen) return true;
    return false;
  }

  static String unquote(String _path){
    byte[] path=_path.getBytes();
    int pathlen=path.length;
    int i=0;
    while(i<pathlen){
      if(path[i]=='\\'){
        if(i+1==pathlen)
          break;
        System.arraycopy(path, i+1, path, i, path.length-(i+1));
        pathlen--;
        continue;
      }
      i++;
    }
    if(pathlen==path.length)return _path;
    byte[] foo=new byte[pathlen];
    System.arraycopy(path, 0, foo, 0, pathlen);
    return new String(foo);
  }

  private static String[] chars={
    "0","1","2","3","4","5","6","7","8","9", "a","b","c","d","e","f"
  };
  static String getFingerPrint(HASH hash, byte[] data){
    try{
      hash.init();
      hash.update(data, 0, data.length);
      byte[] foo=hash.digest();
      StringBuffer sb=new StringBuffer();
      int bar;
      for(int i=0; i<foo.length;i++){
        bar=foo[i]&0xff;
        sb.append(chars[(bar>>>4)&0xf]);
        sb.append(chars[(bar)&0xf]);
        if(i+1<foo.length)
          sb.append(":");
      }
      return sb.toString();
    }
    catch(Exception e){
      return "???";
    }
  }
  static boolean array_equals(byte[] foo, byte bar[]){
    int i=foo.length;
    if(i!=bar.length) return false;
    for(int j=0; j<i; j++){ if(foo[j]!=bar[j]) return false; }
    //try{while(true){i--; if(foo[i]!=bar[i])return false;}}catch(Exception e){}
    return true;
  }
  static Socket createSocket(String host, int port, int timeout) throws JSchException{
    Socket socket=null;
    if(timeout==0){
      try{
        socket=new Socket(host, port);
        return socket;
      }
      catch(Exception e){
        String message=e.toString();
        if(e instanceof Throwable)
          throw new JSchException(message, (Throwable)e);
        throw new JSchException(message);
      }
    }
    final String _host=host;
    final int _port=port;
    final Socket[] sockp=new Socket[1];
    final Exception[] ee=new Exception[1];
    String message="";
    Thread tmp=new Thread(new Runnable(){
        public void run(){
          sockp[0]=null;
          try{
            sockp[0]=new Socket(_host, _port);
          }
          catch(Exception e){
            ee[0]=e;
            if(sockp[0]!=null && sockp[0].isConnected()){
              try{
                sockp[0].close();
              }
              catch(Exception eee){}
            }
            sockp[0]=null;
          }
        }
      });
    tmp.setName("Opening Socket "+host);
    tmp.start();
    try{ 
      tmp.join(timeout);
      message="timeout: ";
    }
    catch(java.lang.InterruptedException eee){
    }
    if(sockp[0]!=null && sockp[0].isConnected()){
      socket=sockp[0];
    }
    else{
      message+="socket is not established";
      if(ee[0]!=null){
        message=ee[0].toString();
      }
      tmp.interrupt();
      tmp=null;
      throw new JSchException(message);
    }
    return socket;
  } 

  static byte[] str2byte(String str){
    if(str==null) 
      return null;
    try{ return str.getBytes("UTF-8"); }
    catch(java.io.UnsupportedEncodingException e){
      return str.getBytes();
    }
  }
  static String byte2str(byte[] str){
    try{ return new String(str, "UTF-8"); }
    catch(java.io.UnsupportedEncodingException e){
      return new String(str);
    }
  }

  /*
  static byte[] char2byte(char[] foo){
    int len=0;
    for(int i=0; i<foo.length; i++){
      if((foo[i]&0xff00)==0) len++;
      else len+=2;
    }
    byte[] bar=new byte[len];
    for(int i=0, j=0; i<foo.length; i++){
      if((foo[i]&0xff00)==0){
        bar[j++]=(byte)foo[i];
      }
      else{
        bar[j++]=(byte)(foo[i]>>>8);
        bar[j++]=(byte)foo[i];
      }
    }
    return bar;
  }
  */
  static void bzero(byte[] foo){
    if(foo==null)
      return;
    for(int i=0; i<foo.length; i++)
      foo[i]=0;
  }
}
