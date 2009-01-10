package org.pzgui;

import org.openflow.util.string.StringOps;

/**
 * Provides some generic GUI helper functions for popping up Dialog boxes.
 * 
 * @author David Underhill
 */
public abstract class DialogHelper {
    /**
     * Displays an error message from a thrown Excpetion
     *
     * @param e  the exception whose message will be shown in a JOptionPane MessageDialog box
     */
    public static void displayError(Exception e) {
        javax.swing.JOptionPane.showMessageDialog(
            null,
            "Error: " + StringOps.splitIntoLines(e.getMessage()),
            "ERROR", javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays an error message
     *
     * @param msg  the message which will be shown in a JOptionPane MessageDialog box
     */
    public static void displayError(String msg) {
        javax.swing.JOptionPane.showMessageDialog(
            null,
            "Error: " + StringOps.splitIntoLines(msg),
            "ERROR", javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays an input dialog box with no default value set
     *
     * @param msg           the message which will be shown in a JOptionPane InputDialog box
     *
     * @return  value specified by the user
     */
    public static String getInput(String msg) {
        return getInput(msg, "");
    }

    /**
     * Displays an input dialog box
     *
     * @param msg           the message which will be shown in a JOptionPane InputDialog box
     * @param defaultValue  the default value for the InputDialog box
     *
     * @return  value specified by the user
     */
    public static String getInput(String msg, String defaultValue) {
        return javax.swing.JOptionPane.showInputDialog(StringOps.splitIntoLines(msg), defaultValue);
    }

    /**
     * Displays an confirm dialog box
     *
     * @param title  the title of the message box
     * @param msg    the message which will be shown in a JOptionPane MessageDialog box
     * @param type   what kind of JOptionPane to use
     *
     * @return  value chosen by the user
     */
    public static int confirmDialog(String title, String msg, int type) {
        return javax.swing.JOptionPane.showConfirmDialog(
            null,
            msg,
            title,
            type,
            javax.swing.JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Displays a message
     *
     * @param msg  the message which will be shown in a JOptionPane MessageDialog box
     */
    public static void displayMessage(String msg) {
        javax.swing.JOptionPane.showMessageDialog(
            null,
            StringOps.splitIntoLines(msg),
            "Alert",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays a message
     *
     * @param title  the title of the message box
     * @param msg    the message which will be shown in a JOptionPane MessageDialog box
     */
    public static void displayMessage(String title, String msg) {
        javax.swing.JOptionPane.showMessageDialog(
            null,
            StringOps.splitIntoLines(msg),
            title,
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }
  
    /**
     * Gets an integer from the user within the specified range.
     * @param msg  The prompt to show the user.
     * @param min  The minimum allowed value.
     * @param def  The default value/
     * @param max  The maximum allowed value.
     * @return  The user-specified value.
     */
    public static int getIntFromUser( String msg, int min, int def, int max ) {
        int v;
        while( true ) {
            try {
                v = Integer.valueOf( getInput(msg, String.valueOf(def)) );
                if( v < min || v > max )
                    displayError( "Error: must be between " + min + " and " + max + "." );
                else
                    return v;
            }
            catch( NumberFormatException e ) {
                displayError(e);
            }
        }
    }
}
