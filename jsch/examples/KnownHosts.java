/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
import com.jcraft.jsch.*;
import javax.swing.*;

public class KnownHosts{
  public static void main(String[] arg){

    try{
      JSch jsch=new JSch();

      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Choose your known_hosts(ex. ~/.ssh/known_hosts)");
      chooser.setFileHidingEnabled(false);
      int returnVal=chooser.showOpenDialog(null);
      if(returnVal==JFileChooser.APPROVE_OPTION) {
        System.out.println("You chose "+
			   chooser.getSelectedFile().getAbsolutePath()+".");
	jsch.setKnownHosts(chooser.getSelectedFile().getAbsolutePath());
      }

      HostKeyRepository hkr=jsch.getHostKeyRepository();
      HostKey[] hks=hkr.getHostKey();
      if(hks!=null){
	System.out.println("Host keys in "+hkr.getKnownHostsRepositoryID());
	for(int i=0; i<hks.length; i++){
	  HostKey hk=hks[i];
	  System.out.println(hk.getHost()+" "+
			     hk.getType()+" "+
			     hk.getFingerPrint(jsch));
	}
	System.out.println("");
      }

      String host=JOptionPane.showInputDialog("Enter username@hostname",
					      System.getProperty("user.name")+
					      "@localhost"); 
      String user=host.substring(0, host.indexOf('@'));
      host=host.substring(host.indexOf('@')+1);

      Session session=jsch.getSession(user, host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);

      session.connect();

      {
	HostKey hk=session.getHostKey();
	System.out.println("HostKey: "+
			   hk.getHost()+" "+
			   hk.getType()+" "+
			   hk.getFingerPrint(jsch));
      }

      Channel channel=session.openChannel("shell");

      channel.setInputStream(System.in);
      channel.setOutputStream(System.out);

      channel.connect();
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


