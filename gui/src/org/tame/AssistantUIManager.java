/* (swing1.1.1) */
package org.tame;

import javax.swing.*;
import javax.swing.plaf.*;


/**
 * @version 1.0 09/08/99
 */
public class AssistantUIManager {

  public static ComponentUI createUI(JComponent c)  {
    String componentName   = c.getClass().getName();
    
    int index = componentName.lastIndexOf(".") +1;
    StringBuffer sb = new StringBuffer();
    sb.append( componentName.substring(0, index) );
    
    //
    // UIManager.getLookAndFeel().getName()
    // 
    // [ Metal ] [  Motif  ] [   Mac   ] [ Windows ]
    //   Metal    CDE/Motif   Macintosh    Windows
    //
    
    String lookAndFeelName = UIManager.getLookAndFeel().getName();
    if (lookAndFeelName.startsWith("CDE/")) {
      lookAndFeelName = lookAndFeelName.substring(4,lookAndFeelName.length());
    }
    sb.append( lookAndFeelName );    
    sb.append( componentName.substring(index) );    
    sb.append( "UI" );    
    
    ComponentUI componentUI = getInstance(sb.toString());
    
    if (componentUI == null) {
      sb.setLength(0);
      sb.append( componentName.substring(0, index) );
      sb.append( "Basic");
      sb.append( componentName.substring(index) );
      sb.append( "UI" );      
      componentUI = getInstance(sb.toString());
    }
    
    return componentUI;
  }
  
  private static ComponentUI getInstance(String name) {
    try {
      return (ComponentUI)Class.forName(name).newInstance();
    } catch (ClassNotFoundException ex) {
    } catch (IllegalAccessException ex) {
      ex.printStackTrace();
    } catch (InstantiationException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  
  public static void setUIName(JComponent c) {
    try {
        UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
    } catch (Exception e) {
        System.err.println("UI not found");
    }
      
    String key = c.getUIClassID();
    String uiClassName = (String)UIManager.get(key);
    
    if (uiClassName == null) {
      String componentName   = c.getClass().getName();    
      int index = componentName.lastIndexOf(".") +1;
      StringBuffer sb = new StringBuffer();
      sb.append( componentName.substring(0, index) );    
      String lookAndFeelName = UIManager.getLookAndFeel().getName();
      if (lookAndFeelName.startsWith("CDE/")) {
        lookAndFeelName = lookAndFeelName.substring(4,lookAndFeelName.length());
      }
      sb.append( lookAndFeelName );    
      sb.append( key );    
      
      UIManager.put("MThumbSliderUI", "org.tame.MetalMThumbSliderUI");
      UIManager.put("MThumbSliderUI", "org.tame.MetalMThumbSliderUI");
    }       
  }
  

  public AssistantUIManager() {
  }
  
  
}