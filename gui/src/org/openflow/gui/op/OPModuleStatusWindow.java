package org.openflow.gui.op;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.HashMap;

import javax.swing.GroupLayout;
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
import org.openflow.gui.net.protocol.op.OPStateField;
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
                        if (f.readOnly)
                            c = new JLabel();
                        else
                            c = new JTextField();
                    }
                    else {
                        throw new UnsupportedOperationException("WARNING: Unimplemented field type encountered");
                    }
                    
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
}
