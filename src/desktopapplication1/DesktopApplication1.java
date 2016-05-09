/*
 * DesktopApplication1.java
 */

package desktopapplication1;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class DesktopApplication1 extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        
//        String msg = "<html>Not all functions work properly because of the migration process<BR>Niet alle functies werken naar behoren vanwege het migratieproces</html>";
//                
//        JLabel label = new JLabel(msg);
//        label.setFont(new Font("serif", Font.PLAIN, 16));
//        JOptionPane.showMessageDialog(null, label, "PROBLEM", JOptionPane.ERROR_MESSAGE); 
        
        show(new DesktopApplication1View(this));
        
        
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
        
        root.addWindowListener(new WindowAdapter() {

        @Override
        public void windowClosing(WindowEvent e) {
            // write your code here

            String msg = "<html>Have you already <u><b>refresh</b></u> database ?</html>";
        
        JLabel label = new JLabel(msg);  
        label.setFont(new Font("serif", Font.PLAIN, 16));  
        int reply = JOptionPane.showConfirmDialog(null, label, "EXIT", JOptionPane.YES_NO_OPTION); 
//            int reply = JOptionPane.showConfirmDialog(null, "Have you already refreshed database?", "EXIT", JOptionPane.YES_NO_OPTION);

                SingleFrameApplication refresh = null;
        if (reply == JOptionPane.NO_OPTION)
            
        {

            DesktopApplication1View method = new DesktopApplication1View(refresh);
            method.refresh();
         }
        }

    });
        
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of DesktopApplication1
     */
    public static DesktopApplication1 getApplication() {
        return Application.getInstance(DesktopApplication1.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(DesktopApplication1.class, args);
    }
}
