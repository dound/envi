package org.openflow.gui.op;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import org.openflow.gui.ConnectionHandler;
import org.openflow.gui.drawables.OPModule;
import org.openflow.gui.net.protocol.op.OPSFInt;
import org.openflow.gui.net.protocol.op.OPSFIntChoice;
import org.openflow.gui.net.protocol.op.OPSVInt;
import org.openflow.gui.net.protocol.op.OPSetStateValues;
import org.openflow.gui.net.protocol.op.OPStateField;
import org.openflow.gui.net.protocol.op.OPStateValue;
import org.openflow.util.Pair;
import org.openflow.util.string.DPIDUtil;
import org.openflow.util.string.IPUtil;

/**
 * Module status window for OpenPipes
 *
 * @author grg
 *
 */
public class OPModuleStatusWindow {
    public static final String TITLE = "Module State Inspector";
    public static final Dimension DEFAULT_SIZE = new Dimension(400, 300);
    public static final int PREFERRED_COMP_WIDTH = 200;

    /** window in which to display the module status */
    private JFrame window;

    /** module being displayed */
    private OPModule module;

    /** Panel to use as the content pane */
    private JPanel contentPane;

    /** Map of names to components */
    private HashMap<String, OPStateField> fieldMap;

    /** Map of names to components */
    private HashMap<String, JComponent> fieldComps;

    /** Map of names to values */
    private HashMap<String, OPStateValue> fieldValues;

    /** Map of components to fields */
    private HashMap<JComponent, OPStateField> compsToFields;

    /** Connection handler to be used for sending messages */
    private ConnectionHandler conn;

    /** Is this window being made visible */
    private boolean makingVisible;

    public OPModuleStatusWindow(ConnectionHandler conn) {
        this.conn = conn;

        window = new JFrame();
        window.setAlwaysOnTop(true);
        window.setSize(DEFAULT_SIZE);

        addWindowFocusListener();
    }

    public void showModule(OPModule m) {
        if (module == m)
            return;

        module = m;
        if (window.isVisible()) {
            updateInternalState();
            updateDisplay();
        }
    }

    public void setVisible(boolean visible) {
        if (window.isVisible() != visible) {
            if (visible) {
                updateInternalState();
                updateDisplay();
            }
            makingVisible = visible;
            window.setVisible(visible);
        }
    }

    public boolean isVisible() {
        return window.isVisible();
    }

    /**
     * Update the internal state
     */
    private void updateInternalState() {
        if (module != null) {
            // Create new maps
            fieldMap = new HashMap<String, OPStateField>();
            fieldComps = new HashMap<String, JComponent>();
            fieldValues = new HashMap<String, OPStateValue>();
            compsToFields = new HashMap<JComponent, OPStateField>();

            // Populate the field map
            for (OPStateField f : module.getStateFields()) {
                fieldMap.put(f.name, f);
            }
        }
        else {
            fieldMap = null;
            fieldComps = null;
            fieldValues = null;
            compsToFields = null;
        }
    }

    /**
     * Update the display to reflect the currently displayed module
     */
    private void updateDisplay() {
        if (module != null) {
            window.setTitle(TITLE + " - " + module.getName());
            OPStateField[] fields = module.getStateFields();
            if (fields.length == 0) {
                createEmptyContentPane("No state information for selected module");
            }
            else {
                contentPane = new JPanel();
                GroupLayout gl = new GroupLayout(contentPane);
                contentPane.setLayout(gl);
                JPanel fieldPane = new JPanel(new SpringLayout());
                GroupLayout.ParallelGroup horizGrp = gl.createParallelGroup(GroupLayout.Alignment.LEADING);
                GroupLayout.SequentialGroup vertGrp = gl.createSequentialGroup();
                horizGrp.addComponent(fieldPane, 0, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE);
                vertGrp.addComponent(fieldPane, 0, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE);
                gl.setHorizontalGroup(horizGrp);
                gl.setVerticalGroup(vertGrp);

                int rows = 0;
                for (OPStateField f : fields) {
                    rows++;

                    JLabel l = new JLabel(f.desc + ":", JLabel.TRAILING);
                    fieldPane.add(l);
                    JComponent c;
                    if (f instanceof OPSFIntChoice) {
                        OPSFIntChoice fIntChoice = (OPSFIntChoice)f;
                        String[] choices = new String[fIntChoice.choices.size()];
                        int pos = 0;
                        for (Pair<Integer, String> p : fIntChoice.choices) {
                            choices[pos++] = p.b;
                        }
                        c = new JComboBox(choices);
                        if (f.readOnly)
                            c.setEnabled(false);
                    }
                    else if (f instanceof OPSFInt) {
                        OPSFInt fInt = (OPSFInt)f;
                        if (fInt.display == OPSFInt.DISP_BOOL) {
                            c = new JCheckBox();
                            c.setEnabled(!f.readOnly);
                        }
                        else {
                            if (f.readOnly)
                                c = new JLabel();
                            else {
                                c = new JTextField();
                            }
                        }
                    }
                    else {
                        throw new UnsupportedOperationException("WARNING: Unimplemented field type encountered");
                    }

                    Dimension d = c.getPreferredSize();
                    d.setSize(PREFERRED_COMP_WIDTH, d.height);
                    c.setPreferredSize(d);
                    addUpdateListeners(c);

                    l.setLabelFor(c);
                    fieldPane.add(c);
                    fieldMap.put(f.name, f);
                    fieldComps.put(f.name, c);
                    compsToFields.put(c, f);
                }

                makeCompactGrid(fieldPane, rows, 2, 6, 6, 6, 6);
                window.setContentPane(contentPane);
                window.validate();
                //                window.pack();
            }
        }
        else {
            window.setTitle(TITLE);
            createEmptyContentPane("No selected module");
        }
    }

    /**
     * Add listeners to a component to ensure that messages
     * are sent to the backend
     *
     * @param c Component to attach listeners to
     */
    private void addUpdateListeners(JComponent c) {
        if (!c.isEnabled())
            // Don't need to do anything if the component is not enabled.
            // In this case the user can't modify it.
            return;

        if (c instanceof JLabel) {
            // Do nothing -- no listeners needed
        }
        else if (c instanceof JTextField) {
            JTextField tf = (JTextField)c;
            tf.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    validateTextField((JTextField)e.getComponent());
                }
            });
            tf.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_ENTER)
                        validateTextField((JTextField)e.getComponent());
                    else if (key == KeyEvent.VK_ESCAPE)
                        resetTextField((JTextField)e.getComponent());
                }
            });
        }
        else if (c instanceof JComboBox) {
            JComboBox cb = (JComboBox)c;
            cb.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    processComboBoxChange((JComboBox)e.getSource());
                }
            });
        }
        else if (c instanceof JCheckBox) {
            JCheckBox cb = (JCheckBox)c;
            cb.addItemListener(new ItemListener() {
                
                @Override
                public void itemStateChanged(ItemEvent e) {
                    processCheckBoxChange((JCheckBox)e.getSource());
                }
            });
        }
    }

    private void createEmptyContentPane(String text) {
        contentPane = new JPanel();
        JLabel notice = new JLabel(text);
        contentPane.add(notice);
        contentPane.doLayout();
        window.setContentPane(contentPane);
    }

    /**
     * Aligns the first <code>rows</code> * <code>cols</code>
     * components of <code>parent</code> in
     * a grid. Each component in a column is as wide as the maximum
     * preferred width of the components in that column;
     * height is similarly determined for each row.
     * The parent is made just big enough to fit them all.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @param initialX x location to start the grid at
     * @param initialY y location to start the grid at
     * @param xPad x padding between cells
     * @param yPad y padding between cells
     */
    private void makeCompactGrid(Container parent,
            int rows, int cols,
            int initialX, int initialY,
            int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout)parent.getLayout();
        } catch (ClassCastException exc) {
            System.err.println("The first argument to makeCompactGrid must use SpringLayout.");
            return;
        }

        //Align all cells in each column and make them the same width.
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width,
                        getConstraintsForCell(r, c, parent, cols).
                        getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints =
                    getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }

        //Align all cells in each row and make them the same height.
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height,
                        getConstraintsForCell(r, c, parent, cols).
                        getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints =
                    getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }

        //Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, y);
        pCons.setConstraint(SpringLayout.EAST, x);
    }

    /* Used by makeCompactGrid. */
    private SpringLayout.Constraints getConstraintsForCell(
            int row, int col,
            Container parent,
            int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }

    /** Update the status values */
    public void setStatusValues(OPSetStateValues msg) {
        if (module.getType() == msg.module.nodeType && module.getID() == msg.module.id) {
            // Walk through the list of values and update them in the GUI
            for (OPStateValue v : msg.values) {
                String name = v.name;
                fieldValues.put(name, v);
                if (v instanceof OPSVInt) {
                    OPSVInt intVal = (OPSVInt)v;
                    OPStateField f = fieldMap.get(name);
                    JComponent c = fieldComps.get(name);
                    if (c == null)
                        throw new UnsupportedOperationException("WARNING: Unknown field '" + name + "'");

                    updateIntField(intVal, f, c);
                }
                else {
                    throw new UnsupportedOperationException("WARNING: Unimplemented value type encountered");
                }
            }
        }
    }

    /** update an integer field */
    private void updateIntField(OPSVInt intVal, OPStateField f, JComponent c) {
        if (c instanceof JLabel) {
            ((JLabel) c).setText(intValToStr(intVal, f, true));
        }
        else if (c instanceof JTextField) {
            ((JTextField) c).setText(intValToStr(intVal, f, false));
        }
        else if (c instanceof JComboBox) {
            ((JComboBox) c).setSelectedIndex(intValToIndex(intVal, f));
        }
        else if (c instanceof JCheckBox) {
            ((JCheckBox) c).setSelected(intVal.value != 0);
        }
    }

    private int intValToIndex(OPSVInt intVal, OPStateField f) {
        OPSFIntChoice fIntChoice = (OPSFIntChoice)f;
        for (int i = 0; i < fIntChoice.choices.size(); i++) {
            Pair<Integer, String> p = fIntChoice.choices.get(i);
            if (p.a.longValue() == intVal.value) {
                return i;
            }
        }
        return 0;
    }

    private String intValToStr(OPSVInt intVal, OPStateField f, boolean longDisplay) {
        OPSFInt fInt = (OPSFInt)f;
        switch (fInt.display) {
            case OPSFInt.DISP_INT:
                if (longDisplay) {
                    if (fInt.width <= 4)
                        return String.format("%d (0x%04x)", intVal.value, intVal.value);
                    else
                        return String.format("%d (0x%08x)", intVal.value, intVal.value);
                }
                else
                    return String.format("%d", intVal.value);

            case OPSFInt.DISP_IP:
                int ipOctet[] = new int[4];
                for (int i = 0; i < 4; i++) {
                    ipOctet[i] = (int)((intVal.value >> ((3 - i) * 8)) & 0xff);
                }
                return String.format("%d.%d.%d.%d", ipOctet[0], ipOctet[1], ipOctet[2], ipOctet[3]);

            case OPSFInt.DISP_MAC:
                int macOctet[] = new int[8];
                for (int i = 0; i < 8; i++) {
                    macOctet[i] = (int)((intVal.value >> ((7 - i) * 8)) & 0xff);
                }
                return String.format("%02x:%02x:%02x:%02x:%02x:%02x",
                        macOctet[2], macOctet[3], macOctet[4],
                        macOctet[5], macOctet[6], macOctet[7]);

            default:
                return String.format("%d", intVal.value);
        }
    }

    public long strToIntVal(String valStr, OPSFInt fInt) {
        switch (fInt.display) {
            case OPSFInt.DISP_IP:
                return IPUtil.stringToIP(valStr);

            case OPSFInt.DISP_MAC:
                return DPIDUtil.hexToDPID(valStr);

            default:
            case OPSFInt.DISP_INT:
                if (fInt.width <= 4)
                    return Integer.valueOf(valStr);
                else
                    return Long.valueOf(valStr);
        }
    }

    private void validateTextField(JTextField tf) {
        String valStr = tf.getText();

        // Get the StateField and StateValue
        OPSFInt fInt = (OPSFInt)compsToFields.get(tf);
        OPStateValue v = fieldValues.get(fInt.name);

        // If the text is non-empty then convert and send a message
        if (!valStr.equals("")) {
            try {
                long value = strToIntVal(valStr, fInt);
                sendSetStateValueInt(fInt, value);
            }
            catch (NumberFormatException nfe) {
                String displayStr;
                if (fInt.display == OPSFInt.DISP_IP)
                    displayStr = "IP address";
                else if (fInt.display == OPSFInt.DISP_MAC)
                    displayStr = "MAC address";
                else
                    displayStr = "integer";

                // Temporarily remove all focus listeners, display a warning
                // then restore the focus listeners
                FocusListener[] listeners = tf.getFocusListeners();
                for (FocusListener listener : listeners)
                    tf.removeFocusListener(listener);
                JOptionPane.showMessageDialog(window,
                        "'" + valStr + "' is not a valid " + displayStr,
                        "Invalid input", JOptionPane.ERROR_MESSAGE);
                for (FocusListener listener : listeners)
                    tf.addFocusListener(listener);

                // Reset the text and set focus to the component
                if (v != null)
                    tf.setText(intValToStr((OPSVInt)v, fInt, false));
                else
                    tf.setText("");
                tf.selectAll();
                tf.requestFocusInWindow();
            }
        }
        else {
            if (v != null) {
                OPSVInt intVal = (OPSVInt)v;
                tf.setText(intValToStr(intVal, fInt, false));
            }
        }
    }
    
    private void sendSetStateValueInt(OPSFInt fInt, long value) {
        // Check to see if the state has actually changed
        if (fieldValues.containsKey(fInt.name)) {
            OPSVInt currVal = (OPSVInt) fieldValues.get(fInt.name);
            if (currVal.value == value)
                return;
        }

        // Create a new OPStateValue
        OPSVInt[] intValues = new OPSVInt[1];
        intValues[0] = new OPSVInt(fInt, value);

        try {
            OPSetStateValues setSVMsg = new OPSetStateValues(
                    new org.openflow.gui.net.protocol.Node(module.getType(), module.getID()),
                    intValues);
            conn.getConnection().sendMessage(setSVMsg);

            fieldValues.put(fInt.name, intValues[0]);
        }
        catch(IOException e) {
            System.err.println("Error: failed to set value on " + fInt.name + " install module due to network error: " + e.getMessage());
        }
    }

    private void resetTextField(JTextField tf) {
        // Get the StateField and StateValue
        OPStateField f = compsToFields.get(tf);
        OPStateValue v = fieldValues.get(f.name);

        // If the text is non-empty then convert and send a message
        if (v != null) {
            OPSVInt intVal = (OPSVInt)v;
            tf.setText(intValToStr(intVal, f, false));
        }
        else {
            tf.setText("");
        }
    }
    
    private void processComboBoxChange(JComboBox cb) {
        OPSFIntChoice fIntChoice = (OPSFIntChoice) compsToFields.get(cb);
        
        Pair<Integer, String> choice = fIntChoice.choices.get(cb.getSelectedIndex());
        sendSetStateValueInt(fIntChoice, choice.a);
    }

    private void processCheckBoxChange(JCheckBox cb) {
        // Get the StateField
        OPSFInt fInt = (OPSFInt) compsToFields.get(cb);
        
        sendSetStateValueInt(fInt, cb.isSelected() ? 1 : 0);
    }

    private void addWindowFocusListener() {
        window.addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowLostFocus(WindowEvent e) {
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (makingVisible)
                    if (e.getOppositeWindow() != null)
                        e.getOppositeWindow().toFront();
                makingVisible = false;
            }
        });
    }
}
