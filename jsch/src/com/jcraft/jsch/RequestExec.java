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

class RequestExec implements Request{
  private String command="";
  public void setCommand(String foo){
    command=foo;
  }
  public void request(Session session, Channel channel) throws Exception{
    Packet packet=session.packet;
    Buffer buf=session.buf;
    // send
    // byte     SSH_MSG_CHANNEL_REQUEST(98)
    // uint32 recipient channel
    // string request type       // "exec"
    // boolean want reply        // 0
    // string command
    packet.reset();
    buf.putByte((byte) Session.SSH_MSG_CHANNEL_REQUEST);
    buf.putInt(channel.getRecipient());
    buf.putString("exec".getBytes());
    buf.putByte((byte)0);
    buf.putString(command.getBytes());
    session.write(packet);
  }
}
