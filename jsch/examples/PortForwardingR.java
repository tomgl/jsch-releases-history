/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
import com.jcraft.jsch.*;
import javax.swing.*;

public class PortForwardingR{
  public static void main(String[] arg){

    int rport;
    String lhost;
    int lport;

    try{
      JSch jsch=new JSch();

      String host=JOptionPane.showInputDialog("Enter username@hostname",
					      System.getProperty("user.name")+
					      "@localhost"); 
      String user=host.substring(0, host.indexOf('@'));
      host=host.substring(host.indexOf('@')+1);

      Session session=jsch.getSession(user, host, 22);

      String foo=JOptionPane.showInputDialog("Enter -R port:host:hostport", 
					     "port:host:hostport");
      rport=Integer.parseInt(foo.substring(0, foo.indexOf(':')));
      foo=foo.substring(foo.indexOf(':')+1);
      lhost=foo.substring(0, foo.indexOf(':'));
      lport=Integer.parseInt(foo.substring(foo.indexOf(':')+1));

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);

      session.connect();

      // Channel channel=session.openChannel("shell");
      // channel.connect();

      session.setPortForwardingR(rport, lhost, lport);

      System.out.println(host+":"+rport+" -> "+lhost+":"+lport);
    }
    catch(Exception e){
      System.out.println(e);
    }
  }

  public static class MyUserInfo implements UserInfo{
    public String getPassword(){ return passwd; }
    public boolean promptYesNo(String str){
      Object[] options={ "yes", "no" };
      int foo=JOptionPane.showOptionDialog(null, 
             str,
             "Warning", 
             JOptionPane.DEFAULT_OPTION, 
             JOptionPane.WARNING_MESSAGE,
             null, options, options[0]);
       return foo==0;
    }
  
    String passwd;
    JTextField passwordField=(JTextField)new JPasswordField(20);

    public String getPassphrase(){ return null; }
    public boolean promptPassphrase(String message){ return true; }
    public boolean promptPassword(String message){
      Object[] ob={passwordField}; 
      int result=
	  JOptionPane.showConfirmDialog(null, ob, message,
					JOptionPane.OK_CANCEL_OPTION);
      if(result==JOptionPane.OK_OPTION){
	passwd=passwordField.getText();
	return true;
      }
      else{ return false; }
    }
    public void showMessage(String message){
      JOptionPane.showMessageDialog(null, message);
    }
  }

}
