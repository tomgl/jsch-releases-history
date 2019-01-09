/* -*-mode:java; c-basic-offset:2; -*- */
/* JSch
 * Copyright (C) 2002 ymnk, JCraft,Inc.
 *  
 * Written by: 2002 ymnk<ymnk@jcaft.com>
 *   
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.jcraft.jsch;

import java.io.*;
import java.net.*;

public class ProxyHTTP implements Proxy{
  private String proxy_host;
  private int proxy_port;
  private String host;
  private int port;
  private InputStream in;
  private OutputStream out;
  private Socket socket;

  private String user;
  private String passwd;

  public ProxyHTTP(String proxy_host){
    this(proxy_host, 80);
  }
  public ProxyHTTP(String proxy_host, int proxy_port){
    this.proxy_host=proxy_host;
    this.proxy_port=proxy_port;
  }
  public void setUserPasswd(String user, String passwd){
    this.user=user;
    this.passwd=passwd;
  }
  public void connect(Session session, String host, int port) throws JSchException{
    this.host=host;
    this.port=port;
    try{
      SocketFactory socket_factory=session.socket_factory;
      if(socket_factory==null){
        socket=new Socket(proxy_host, proxy_port);    
        in=socket.getInputStream();
        out=socket.getOutputStream();
      }
      else{
        socket=socket_factory.createSocket(proxy_host, proxy_port);
        in=socket_factory.getInputStream(socket);
        out=socket_factory.getOutputStream(socket);
      }
      socket.setTcpNoDelay(true);
      out.write(("CONNECT "+host+":"+port+" HTTP/1.0\n").getBytes());

      out.write("\n".getBytes());
      out.flush();

      int foo;
      while(true){
        foo=in.read(); if(foo!=13) continue;
        foo=in.read(); if(foo!=10) continue;
        foo=in.read(); if(foo!=13) continue;      
        foo=in.read(); if(foo!=10) continue;
        break;
      }
    }
    catch(Exception e){
      try{ if(socket!=null)socket.close(); }
      catch(Exception eee){
      }
      throw new JSchException("ProxyHTTP: "+e.toString());
    }
  }
  public InputStream getInputStream(){ return in; }
  public OutputStream getOutputStream(){ return out; }
  public void close(){
    try{
      if(in!=null)in.close();
      if(out!=null)out.close();
      if(socket!=null)socket.close();
    }
    catch(Exception e){
    }
    in=null;
    out=null;
    socket=null;
  }
}
