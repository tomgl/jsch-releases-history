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

class UserAuthNone extends UserAuth{
  private String methods=null;

  public boolean start(Session session, UserInfo userinfo) throws Exception{
    super.start(session, userinfo);
//System.err.println("UserAuthNone: start");
    Packet packet=session.packet;
    Buffer buf=session.buf;
    final String username=session.username;

    byte[] _username=null;
    _username=Util.str2byte(username);

    // send
    // byte      SSH_MSG_USERAUTH_REQUEST(50)
    // string    user name
    // string    service name ("ssh-connection")
    // string    "none"
    packet.reset();
    buf.putByte((byte)SSH_MSG_USERAUTH_REQUEST);
    buf.putString(_username);
    buf.putString("ssh-connection".getBytes());
    buf.putString("none".getBytes());
    session.write(packet);

    loop:
    while(true){
      // receive
      // byte      SSH_MSG_USERAUTH_SUCCESS(52)
      // string    service name
      buf=session.read(buf);
      // System.err.println("UserAuthNone: read: 52 ? "+    buf.buffer[5]);
      if(buf.buffer[5]==SSH_MSG_USERAUTH_SUCCESS){
	return true;
      }
      if(buf.buffer[5]==SSH_MSG_USERAUTH_BANNER){
	buf.getInt(); buf.getByte(); buf.getByte();
	byte[] _message=buf.getString();
	byte[] lang=buf.getString();
	String message=null;
	try{ 
          message=new String(_message, "UTF-8"); 
        }
	catch(java.io.UnsupportedEncodingException e){
	  message=new String(_message);
	}
	if(userinfo!=null){
          try{
            userinfo.showMessage(message);
          }
          catch(RuntimeException ee){
          }
	}
	continue loop;
      }
      if(buf.buffer[5]==SSH_MSG_USERAUTH_FAILURE){
	buf.getInt(); buf.getByte(); buf.getByte(); 
	byte[] foo=buf.getString();
	int partial_success=buf.getByte();
	methods=new String(foo);
//System.err.println("UserAuthNONE: "+methods+
//		   " partial_success:"+(partial_success!=0));
//	if(partial_success!=0){
//	  throw new JSchPartialAuthException(new String(foo));
//	}

        break;
      }
      else{
//      System.err.println("USERAUTH fail ("+buf.buffer[5]+")");
	throw new JSchException("USERAUTH fail ("+buf.buffer[5]+")");
      }
    }
   //throw new JSchException("USERAUTH fail");
    return false;
  }
  String getMethods(){
    return methods;
  }
}
