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

public class JSch{
  static java.util.Properties config=new java.util.Properties();
  static{
    config.put("random", "com.jcraft.jsch.jce.Random");
//  config.put("kex", "diffie-hellman-group-exchange-sha1");
    config.put("kex", "diffie-hellman-group1-sha1");
    config.put("dh", "com.jcraft.jsch.jce.DH");
    config.put("server_host_key", "ssh-rsa,ssh-dss");
//  config.put("server_host_key", "ssh-dss,ssh-rsa");
    config.put("cipher.s2c", "blowfish-cbc");
    config.put("cipher.c2s", "blowfish-cbc");
    config.put("mac.s2c", "hmac-md5");
    config.put("mac.c2s", "hmac-md5");
    config.put("compression.s2c", "none");
    config.put("compression.c2s", "none");
    config.put("lang.s2c", "");
    config.put("lang.c2s", "");

    config.put("diffie-hellman-group-exchange-sha1", "com.jcraft.jsch.jce.DHGEX");
    config.put("diffie-hellman-group1-sha1", "com.jcraft.jsch.jce.DHG1");

    config.put("3des-cbc",      "com.jcraft.jsch.jce.TripleDESCBC");
    config.put("blowfish-cbc",  "com.jcraft.jsch.jce.BlowfishCBC");
    config.put("hmac-sha1",     "com.jcraft.jsch.jce.HMACSHA1");
    config.put("hmac-sha1-96",  "com.jcraft.jsch.jce.HMACSHA196");
    config.put("hmac-md5",      "com.jcraft.jsch.jce.HMACMD5");
    config.put("hmac-md5-96",   "com.jcraft.jsch.jce.HMACMD596");
    config.put("sha-1",         "com.jcraft.jsch.jce.SHA1");
    config.put("md5",           "com.jcraft.jsch.jce.MD5");
    config.put("signature.dss", "com.jcraft.jsch.jce.SignatureDSA");
    config.put("signature.rsa", "com.jcraft.jsch.jce.SignatureRSA");

    config.put("zlib", "com.jcraft.jsch.jcraft.Compression");
  }
  private static java.util.Vector pool=new java.util.Vector();
  static java.util.Vector identities=new java.util.Vector();
  private KnownHosts known_hosts=null;

  public JSch(){
    known_hosts=new KnownHosts();
  }
  public Session getSession(String username, String host) throws JSchException { return getSession(username, host, 22); }
  public Session getSession(String username, String host, int port) throws JSchException {
    Session s=new Session(this); 
    s.setUserName(username);
    s.setHost(host);
    s.setPort(port);
    pool.addElement(s);
    return s;
  }
  public void setKnownHosts(String foo){ known_hosts.setKnownHosts(foo); }
  public KnownHosts getKnownHosts(){ return known_hosts; }
  public void addIdentity(String foo) throws Exception{
    addIdentity(foo, null);
  }
  public void addIdentity(String foo, String bar) throws Exception{
    Identity identity=new Identity(foo, this);
    if(bar!=null) identity.setPassphrase(bar);
    identities.addElement(identity);
  }
  String getConfig(String foo){ return (String)(config.get(foo)); }
}
