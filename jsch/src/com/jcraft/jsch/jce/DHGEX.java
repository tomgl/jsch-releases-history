/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2002,2003 ymnk, JCraft,Inc. All rights reserved.

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
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
VISIGOTH SOFTWARE SOCIETY OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.jcraft.jsch.jce;

import com.jcraft.jsch.*;

public class DHGEX extends KeyExchange{
  static int min=1024;

//  static int min=512;
  static int preferred=1024;
  static int max=1024;

//  static int preferred=1024;
//  static int max=2000;

  static final int RSA=0;
  static final int DSS=1;
  private int type=0;

  com.jcraft.jsch.DH dh;
  HASH sha;

  byte[] K;
  byte[] H;

  byte[] V_S;
  byte[] V_C;
  byte[] I_S;
  byte[] I_C;

  byte[] K_S;

  public void init(byte[] V_S, byte[] V_C, byte[] I_S, byte[] I_C) throws Exception{
    this.V_S=V_S;      
    this.V_C=V_C;      
    this.I_S=I_S;      
    this.I_C=I_C;      

    sha=new SHA1();
    sha.init();
  }

  public boolean start(Session session) throws Exception{

    try{
      Class c=Class.forName(session.getConfig("dh"));
      dh=(com.jcraft.jsch.DH)(c.newInstance());
      dh.init();
    }
    catch(Exception e){
      System.err.println(e);
    }

    int i,j;
    Buffer buf=new Buffer();
    Packet packet=new Packet(buf);

    packet.reset();
    buf.putByte((byte)0x22);
    buf.putInt(min);
    buf.putInt(preferred);
    buf.putInt(max);
    session.write(packet); 

    // byte  SSH_MSG_KEX_DH_GEX_GROUP(31)
    // mpint p, safe prime
    // mpint g, generator for subgroup in GF (p)
    buf=session.read(buf);
    buf.getInt();
    buf.getByte();
j=    buf.getByte();
if(j!=31){
  System.err.println("type: must be 31 "+j);
  return false;
}
    byte[] p=buf.getMPInt();
    byte[] g=buf.getMPInt();

for(int iii=0; iii<p.length; iii++){
System.out.println("0x"+Integer.toHexString(p[iii]&0xff)+",");
}
System.out.println("");
for(int iii=0; iii<g.length; iii++){
System.out.println("0x"+Integer.toHexString(g[iii]&0xff)+",");
}

    dh.setP(p);
    dh.setG(g);

    // The client responds with:
    // byte  SSH_MSG_KEX_DH_GEX_INIT(32)
    // mpint e <- g^x mod p
    //         x is a random number (1 < x < (p-1)/2)

    byte[] e=dh.getE();

    packet.reset();
    buf.putByte((byte)0x20);
    buf.putMPInt(e);
    session.write(packet);

    // The server responds with:
    // byte      SSH_MSG_KEX_DH_GEX_REPLY(33)
    // string    server public host key and certificates (K_S)
    // mpint     f
    // string    signature of H
    buf=session.read(buf);
    j=buf.getInt();
    j=buf.getByte();
    j=buf.getByte();
if(j!=33){
System.err.println("type: must be 33 "+j);
return false;
}

    K_S=buf.getString();
    // K_S is server_key_blob, which includes ....
    // string ssh-dss
    // impint p of dsa
    // impint q of dsa
    // impint g of dsa
    // impint pub_key of dsa
    //System.out.print("K_S: "); dump(K_S, 0, K_S.length);

    byte[] f=buf.getMPInt();
    byte[] sig_of_H=buf.getString();

    dh.setF(f);
    K=dh.getK();

    //The hash H is computed as the HASH hash of the concatenation of the
    //following:
    // string    V_C, the client's version string (CR and NL excluded)
    // string    V_S, the server's version string (CR and NL excluded)
    // string    I_C, the payload of the client's SSH_MSG_KEXINIT
    // string    I_S, the payload of the server's SSH_MSG_KEXINIT
    // string    K_S, the host key
    // uint32    min, minimal size in bits of an acceptable group
    // uint32   n, preferred size in bits of the group the server should send
    // uint32    max, maximal size in bits of an acceptable group
    // mpint     p, safe prime
    // mpint     g, generator for subgroup
    // mpint     e, exchange value sent by the client
    // mpint     f, exchange value sent by the server
    // mpint     K, the shared secret
    // This value is called the exchange hash, and it is used to authenti-
    // cate the key exchange.

    buf.reset();
    buf.putString(V_C); buf.putString(V_S);
    buf.putString(I_C); buf.putString(I_S);
    buf.putString(K_S);
    buf.putInt(min); buf.putInt(preferred); buf.putInt(max);
    buf.putMPInt(p); buf.putMPInt(g); buf.putMPInt(e); buf.putMPInt(f);
    buf.putMPInt(K);

    byte[] foo=new byte[buf.getLength()];
    buf.getByte(foo);
    sha.update(foo, 0, foo.length);

    H=sha.digest();

    // System.out.print("H -> "); dump(H, 0, H.length);

    i=0;
    j=0;
    j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
      ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
    String alg=new String(K_S, i, j);
    i+=j;

    if(alg.equals("ssh-rsa")){
      byte[] tmp;
      byte[] ee;
      byte[] n;

      type=RSA;

      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      ee=tmp;
      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      n=tmp;

      SignatureRSA sig=new SignatureRSA();
      sig.init();
      sig.setPubKey(ee, n);   
      sig.update(H);
      return sig.verify(sig_of_H);
    }
    else if(alg.equals("ssh-dss")){
      byte[] q=null;
      byte[] tmp;

      type=DSS;

      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      p=tmp;
      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      q=tmp;
      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      g=tmp;
      j=((K_S[i++]<<24)&0xff000000)|((K_S[i++]<<16)&0x00ff0000)|
	  ((K_S[i++]<<8)&0x0000ff00)|((K_S[i++])&0x000000ff);
      tmp=new byte[j]; System.arraycopy(K_S, i, tmp, 0, j); i+=j;
      f=tmp;

      SignatureDSA sig=new SignatureDSA();
      sig.init();
      sig.setPubKey(f, p, q, g);   
      sig.update(H);
      return sig.verify(sig_of_H);
    }
    else{
	System.out.println("unknow alg");
	return false;
    }	    

  }

  public byte[] getK(){ return K; }
  public byte[] getH(){ return H; }
  public HASH getHash(){ return sha; }
  public byte[] getHostKey(){ return K_S; }

  static String[] chars={
    "0","1","2","3","4","5","6","7","8","9", "a","b","c","d","e","f"
  };
  public String getFingerPrint(){
    try{
      java.security.MessageDigest md=java.security.MessageDigest.getInstance("MD5");
      md.update(K_S, 0, K_S.length);
      byte[] foo=md.digest();
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
  public String getKeyType(){
    if(type==DSS) return "DSA";
    return "RSA";
  }
}
