package odme.odmeeditor;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;



public class SplashScreen {
	
    private JFrame frame;
    private JLabel image;
    private JLabel text;
    private JProgressBar progressBar;
    private JLabel message;
    private int duration_sec;
    
    public SplashScreen(int duration) {
    	duration_sec = duration;
    	image = new JLabel(new ImageIcon(
    			ODMEEditor.class.getClassLoader().getResource("images/tu_clausthal_logo.png")));
    	text=new JLabel("   Operation Domain Modeling Environment");
    	progressBar=new JProgressBar();
    	message=new JLabel("© Copyright 2022 by TU Clausthal, Informatik institute. All rights reserved.");
    			
        createGUI();
        addImage();
        addText();
        addProgressBar();
        addMessage();
    }
    public void createGUI() {
        frame=new JFrame();
        frame.getContentPane().setLayout(null);
        frame.setUndecorated(true);
        frame.setSize(600,380);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(Color.white);
        frame.getRootPane().setBorder(
                BorderFactory.createMatteBorder(8, 1, 1, 1, Color.ORANGE));
        frame.setVisible(true);
    }
    
    public void addImage() {
        image.setSize(600,230);
        frame.add(image);
    }
    
    public void addText() {
        text.setFont(new Font("arial",Font.BOLD,26));
        text.setBounds(15,230,600,40);
        text.setForeground(new Color(0x04773b));
        frame.add(text);
    }
    
    public void addMessage() {
        message.setBounds(15,325,450,40);//Setting the size and location of the label
        message.setForeground(Color.black);//Setting foreground Color
        //message.setFont(new Font("arial",Font.BOLD,15));//Setting font properties
        frame.add(message);//adding label to the frame
    }
    
    public void addProgressBar() {
        progressBar.setBounds(100,280,400,30);
        progressBar.setBorderPainted(true);
        progressBar.setStringPainted(true);
        progressBar.setBackground(Color.WHITE);
        progressBar.setForeground(new Color(0x04773b));
        progressBar.setValue(0);
        frame.add(progressBar);
    }
    
    public void runningPBar() {
        int i=0;//Creating an integer variable and intializing it to 0

        while( i<=100){
            try{
                Thread.sleep(duration_sec*10);//Pausing execution for 50 milliseconds
                progressBar.setValue(i);//Setting value of Progress Bar
                i++;
                if(i==100)
                    frame.dispose();
                }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
