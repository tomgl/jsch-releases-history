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

/*
 This file depends on following documents,
   - RFC 1928  SOCKS Protocol Verseion 5  
   - RFC 1929  Username/Password Authentication for SOCKS V5. 
 */

package com.jcraft.jsch;

import java.io.*;
import java.net.*;

public class ProxySOCKS5 implements Proxy{
  private String proxy_host;
  private int proxy_port;
  private String host;
  private int port;
  private InputStream in;
  private OutputStream out;
  private Socket socket;
  private String user;
  private String passwd;

  public ProxySOCKS5(String proxy_host){
    this(proxy_host, 1080);
  }
  public ProxySOCKS5(String proxy_host, int proxy_port){
    this.proxy_host=proxy_host;
    this.proxy_port=proxy_port;
  }
  public void setUserPasswd(String user, String passwd){
    this.user=user;
    this.passwd=passwd;
  }
  public void connect(String host, int port) throws JSchException{
    this.host=host;
    this.port=port;
    try{
      socket=new Socket(proxy_host, proxy_port);    
      socket.setTcpNoDelay(true);
      in=socket.getInputStream();
      out=socket.getOutputStream();

      byte[] buf=new byte[1024];
      int index=0;

/*
                   +----+----------+----------+
                   |VER | NMETHODS | METHODS  |
                   +----+----------+----------+
                   | 1  |    1     | 1 to 255 |
                   +----+----------+----------+

   The VER field is set to X'05' for this version of the protocol.  The
   NMETHODS field contains the number of method identifier octets that
   appear in the METHODS field.

   The values currently defined for METHOD are:

          o  X'00' NO AUTHENTICATION REQUIRED
          o  X'01' GSSAPI
          o  X'02' USERNAME/PASSWORD
          o  X'03' to X'7F' IANA ASSIGNED
          o  X'80' to X'FE' RESERVED FOR PRIVATE METHODS
          o  X'FF' NO ACCEPTABLE METHODS
*/

      buf[index++]=5;

      buf[index++]=2;
      buf[index++]=0;           // NO AUTHENTICATION REQUIRED
      buf[index++]=2;           // USERNAME/PASSWORD

      out.write(buf, 0, index);

/*
    The server selects from one of the methods given in METHODS, and
    sends a METHOD selection message:

                         +----+--------+
                         |VER | METHOD |
                         +----+--------+
                         | 1  |   1    |
                         +----+--------+
*/
      in.read(buf, 0, 2);
 
      boolean check=false;
      switch((buf[1])&0xff){
        case 0:                // NO AUTHENTICATION REQUIRED
          check=true;
          break;
        case 2:                // USERNAME/PASSWORD
          if(user==null || passwd==null)break;

/*
   Once the SOCKS V5 server has started, and the client has selected the
   Username/Password Authentication protocol, the Username/Password
   subnegotiation begins.  This begins with the client producing a
   Username/Password request:

           +----+------+----------+------+----------+
           |VER | ULEN |  UNAME   | PLEN |  PASSWD  |
           +----+------+----------+------+----------+
           | 1  |  1   | 1 to 255 |  1   | 1 to 255 |
           +----+------+----------+------+----------+

   The VER field contains the current version of the subnegotiation,
   which is X'01'. The ULEN field contains the length of the UNAME field
   that follows. The UNAME field contains the username as known to the
   source operating system. The PLEN field contains the length of the
   PASSWD field that follows. The PASSWD field contains the password
   association with the given UNAME.
*/
          index=0;
          buf[index++]=1;
          buf[index++]=(byte)(user.length());
	  System.arraycopy(user.getBytes(), 0, buf, index, user.length());
	  index+=user.length();
          buf[index++]=(byte)(passwd.length());
	  System.arraycopy(passwd.getBytes(), 0, buf, index, passwd.length());
	  index+=passwd.length();

          out.write(buf, 0, index);

/*
   The server verifies the supplied UNAME and PASSWD, and sends the
   following response:

                        +----+--------+
                        |VER | STATUS |
                        +----+--------+
                        | 1  |   1    |
                        +----+--------+

   A STATUS field of X'00' indicates success. If the server returns a
   `failure' (STATUS value other than X'00') status, it MUST close the
   connection.
*/
          in.read(buf, 0, 2);
          if(buf[1]==0)
            check=true;
          break;
        default:
      }

      if(!check){
        try{ socket.close(); }
	catch(Exception eee){
	}
        throw new JSchException("fail in SOCKS5 proxy");
      }

/*
      The SOCKS request is formed as follows:

        +----+-----+-------+------+----------+----------+
        |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
        +----+-----+-------+------+----------+----------+
        | 1  |  1  | X'00' |  1   | Variable |    2     |
        +----+-----+-------+------+----------+----------+

      Where:

      o  VER    protocol version: X'05'
      o  CMD
         o  CONNECT X'01'
         o  BIND X'02'
         o  UDP ASSOCIATE X'03'
      o  RSV    RESERVED
         o  ATYP   address type of following address
         o  IP V4 address: X'01'
         o  DOMAINNAME: X'03'
         o  IP V6 address: X'04'
      o  DST.ADDR       desired destination address
      o  DST.PORT desired destination port in network octet
         order
*/
     
      index=0;
      buf[index++]=5;
      buf[index++]=1;       // CONNECT
      buf[index++]=0;

      byte[] hostb=host.getBytes();
      int len=hostb.length;
      buf[index++]=3;      // DOMAINNAME
      buf[index++]=(byte)(len);
      System.arraycopy(hostb, 0, buf, index, len);
      index+=len;
      buf[index++]=(byte)(port>>>8);
      buf[index++]=(byte)(port&0xff);

      out.write(buf, 0, index);

/*
   The SOCKS request information is sent by the client as soon as it has
   established a connection to the SOCKS server, and completed the
   authentication negotiations.  The server evaluates the request, and
   returns a reply formed as follows:

        +----+-----+-------+------+----------+----------+
        |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
        +----+-----+-------+------+----------+----------+
        | 1  |  1  | X'00' |  1   | Variable |    2     |
        +----+-----+-------+------+----------+----------+

   Where:

   o  VER    protocol version: X'05'
   o  REP    Reply field:
      o  X'00' succeeded
      o  X'01' general SOCKS server failure
      o  X'02' connection not allowed by ruleset
      o  X'03' Network unreachable
      o  X'04' Host unreachable
      o  X'05' Connection refused
      o  X'06' TTL expired
      o  X'07' Command not supported
      o  X'08' Address type not supported
      o  X'09' to X'FF' unassigned
    o  RSV    RESERVED
    o  ATYP   address type of following address
      o  IP V4 address: X'01'
      o  DOMAINNAME: X'03'
      o  IP V6 address: X'04'
    o  BND.ADDR       server bound address
    o  BND.PORT       server bound port in network octet order
*/

      in.read(buf, 0, 4);

      if(buf[1]!=0){
        try{ socket.close(); }
	catch(Exception eee){
	}
        throw new JSchException("ProxySOCKS5: server returns "+buf[1]);
      }

      switch(buf[3]&0xff){
        case 1:
          in.read(buf, 0, 6);
	  break;
        case 3:
          in.read(buf, 0, 1);
          in.read(buf, 0, buf[0]+2);
	  break;
        case 4:
          in.read(buf, 0, 18);
          break;
        default:
      }
    }
    catch(Exception e){
      try{ if(socket!=null)socket.close(); }
      catch(Exception eee){
      }
      throw new JSchException("ProxySOCKS5: "+e.toString());
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
