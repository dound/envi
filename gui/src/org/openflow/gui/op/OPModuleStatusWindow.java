package org.openflow.gui.op;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.HashMap;

import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import org.openflow.gui.drawables.OPModule;
import org.openflow.gui.net.protocol.op.OPSTInt;
import org.openflow.gui.net.protocol.op.OPSTIntChoice;
import org.openflow.gui.net.protocol.op.OPSVInt;
import org.openflow.gui.net.protocol.op.OPSetStateValues;
import org.openflow.gui.net.protocol.op.OPStateField;
import org.openflow.gui.net.protocol.op.OPStateValue;
import org.openflow.util.Pair;

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
    
    /** Map of components to fields */
    private HashMap<String, Pair<OPStateField, JComponent>> fieldMap;
    
    public OPModuleStatusWindow() {
        window = new JFrame();
        window.setAlwaysOnTop(true);
        window.setSize(DEFAULT_SIZE);
    }
    
    public void showModule(OPModule m) {
        if (module == m)
            return;
        
        module = m;
        if (window.isVisible()) {
            updateDisplay();
        }
    }
    
    public void setVisible(boolean visible) {
        if (window.isVisible() != visible) {
            if (visible)
                updateDisplay();
            window.setVisible(visible);
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
                
                fieldMap = new HashMap<String, Pair<OPStateField, JComponent>>();
                int rows = 0;
                for (OPStateField f : fields) {
                    rows++;
                    
                    JLabel l = new JLabel(f.desc, JLabel.TRAILING);
                    fieldPane.add(l);
                    JComponent c;
                    if (f.type instanceof OPSTIntChoice) {
                        OPSTIntChoice ic = (OPSTIntChoice)f.type;
                        String[] choices = new String[ic.choices.size()];
                        int pos = 0;
                        for (Pair<Integer, String> p : ic.choices) {
                            choices[pos++] = p.b;
                        }
                        c = new JComboBox(choices);
                        if (f.readOnly)
                            c.setEnabled(false);
                    }
                    else if (f.type instanceof OPSTInt) {
                        OPSTInt intType = (OPSTInt)f.type;
                        if (intType.display == OPSTInt.DISP_BOOL) {
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

                    l.setLabelFor(c);
                    fieldPane.add(c);
                    fieldMap.put(f.name, new Pair<OPStateField, JComponent>(f, c));
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
        System.out.println(msg.module.toString());
        System.out.println(module.toString());
        if (module.getType() == msg.module.nodeType && module.getID() == msg.module.id) {
            // Walk through the list of values and update them in the GUI
            for (OPStateValue v : msg.values) {
                if (v instanceof OPSVInt) {
                    OPSVInt intVal = (OPSVInt)v;
                    String name = intVal.name;
                    Pair<OPStateField, JComponent> p = fieldMap.get(name);
                    OPStateField f = p.a;
                    JComponent c = p.b;
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
            ((JLabel) c).setText(intValToStr(intVal, f, false));
        }
        else if (c instanceof JTextField) {
            ((JTextField) c).setText(intValToStr(intVal, f, true));
        }
        else if (c instanceof JComboBox) {
            ((JComboBox) c).setSelectedIndex(intValToIndex(intVal, f));
        }
        else if (c instanceof JCheckBox) {
            ((JCheckBox) c).setSelected(intVal.value != 0);
        }
    }

    private int intValToIndex(OPSVInt intVal, OPStateField f) {
        OPSTIntChoice type = (OPSTIntChoice)f.type;
        for (int i = 0; i < type.choices.size(); i++) {
            Pair<Integer, String> p = type.choices.get(i);
            if (p.a.longValue() == intVal.value) {
                return i;
            }
        }
        return 0;
    }

    private String intValToStr(OPSVInt intVal, OPStateField f, boolean longDisplay) {
        OPSTInt type = (OPSTInt)f.type;
        switch (type.display) {
            case OPSTInt.DISP_INT:
                if (longDisplay) {
                    if (type.width <= 4)
                        return String.format("%d (0x%04x)", intVal.value, intVal.value);
                    else
                        return String.format("%d (0x%08x)", intVal.value, intVal.value);
                }
                else
                    return String.format("%d", intVal.value);

            case OPSTInt.DISP_IP:
                int ipOctet[] = new int[4];
                for (int i = 0; i < 4; i++) {
                    ipOctet[i] = (int)((intVal.value >> ((3 - i) * 8)) & 0xff);
                }
                return String.format("%d.%d.%d.%d", ipOctet[0], ipOctet[1], ipOctet[2], ipOctet[3]);

            case OPSTInt.DISP_MAC:
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
}
