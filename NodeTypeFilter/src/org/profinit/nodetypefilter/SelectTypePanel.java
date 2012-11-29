/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.profinit.nodetypefilter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicListUI;
import org.gephi.partition.api.Part;
import org.gephi.partition.api.Partition;

/**
 * Upraveny panel, pouzivany pro puvodni filtrovani podle typu uzlu
 * @author Mathieu Bastian, Petr Hlavacek
 */
public class SelectTypePanel extends javax.swing.JPanel {

    private final NodeTypeFilter filter;
    private JPopupMenu popupMenu;

    public SelectTypePanel(NodeTypeFilter f) {
        this.filter = f;
        initComponents();
        setMinimumSize(new Dimension(50, 90));

        //List renderer
        final ListCellRenderer renderer = new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {

                final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                PartWrapper pw = (PartWrapper) value;
                if (pw.isEnabled()) {
                    label.setEnabled(true);
                    label.setIcon(pw.icon);
                } else {
                    label.setEnabled(false);
                    label.setDisabledIcon(pw.disabledIcon);
                }
                label.setFont(label.getFont().deriveFont(10f));
                label.setIconTextGap(6);
                setOpaque(false);
                setForeground(list.getForeground());
                setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
                return label;
            }
        };
        list.setCellRenderer(renderer);

        //List click
        MouseListener mouseListener = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index == -1) {
                        return;
                    }
                    PartWrapper pw = (PartWrapper) list.getModel().getElementAt(index);
                    boolean set = !pw.isEnabled();
                    pw.setEnabled(set);
                    if (set) {
                        filter.addPart(pw.getPart());
                    } else {
                        filter.removePart(pw.getPart());
                    }
                    list.repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (filter != null) {
                    if (e.isPopupTrigger()) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
        list.addMouseListener(mouseListener);

        //Popup
        createPopup();
        setup();
    }
    /**
     * 
     */
    public void setup() {
        
        final Partition partition = filter.getCurrentPartition();
        if (partition != null) {
            refresh(partition, filter.getParts());
        }
    }
    /**
     * Obnoveni nabidky
     * @param partition
     * @param currentParts 
     */
    private void refresh(Partition partition, List<Part> currentParts) {
        final DefaultListModel model = new DefaultListModel();

        Set<Part> filterParts = new HashSet<Part>(currentParts);
        Part[] parts = partition.getParts();
        Arrays.sort(parts);
        for (int i = 0; i < parts.length; i++) {
            final Part p = parts[parts.length - 1 - i];
            PartWrapper pw = new PartWrapper(p, p.getColor());
            pw.setEnabled(filterParts.contains(p));
            model.add(i, pw);
        }
        list.setModel(model);
    }
    /**
     * Vytvari componentu s nazvem moznosti pro filtrovani a check boxem, ktery
     * ji umoznuje zaskrtnout
     */
    private static class PartWrapper {

        private final Part part;
        private final PaletteIcon icon;
        private final PaletteIcon disabledIcon;
        private boolean enabled = false;
        private static final NumberFormat formatter = NumberFormat.getPercentInstance();

        public PartWrapper(Part part, Color color) {
            this.part = part;
            this.icon = new PaletteIcon(color);
            this.disabledIcon = new PaletteIcon();
            formatter.setMaximumFractionDigits(2);
        }

        public PaletteIcon getIcon() {
            return icon;
        }

        public Part getPart() {
            return part;
        }

        @Override
        public String toString() {
            String percentage = formatter.format(part.getPercentage());
            return part.getDisplayName() + " (" + percentage + ")";
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    private void createPopup() {
        popupMenu = new JPopupMenu();
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setup();
            }
        });
        popupMenu.add(refreshItem);
        JMenuItem selectItem = new JMenuItem("Select All");
        selectItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                filter.selectAll();
                refresh(filter.getCurrentPartition(), Arrays.asList(filter.getCurrentPartition().getParts()));
            }
        });
        popupMenu.add(selectItem);
        JMenuItem unselectItem = new JMenuItem("Unselect all");
        unselectItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                filter.unselectAll();
                refresh(filter.getCurrentPartition(), new ArrayList<Part>());
            }
        });
        popupMenu.add(unselectItem);
    }

    public static void computeListSize(final JList list) {
        if (list.getUI() instanceof BasicListUI) {
            final BasicListUI ui = (BasicListUI) list.getUI();

            try {
                final Method method = BasicListUI.class.getDeclaredMethod("updateLayoutState");
                method.setAccessible(true);
                method.invoke(ui);
                list.revalidate();
                list.repaint();
            } catch (final SecurityException e) {
                e.printStackTrace();
            } catch (final NoSuchMethodException e) {
                e.printStackTrace();
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            } catch (final InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private static class PaletteIcon implements Icon {

        private final int COLOR_WIDTH;
        private final int COLOR_HEIGHT;
        private final Color BORDER_COLOR;
        private final Color color;

        public PaletteIcon(Color color) {
            this.color = color;
            BORDER_COLOR = new Color(0x444444);
            COLOR_WIDTH = 11;
            COLOR_HEIGHT = 11;
        }

        public PaletteIcon() {
            this.color = new Color(0xDDDDDD);
            BORDER_COLOR = new Color(0x999999);
            COLOR_WIDTH = 11;
            COLOR_HEIGHT = 11;
        }

        public int getIconWidth() {
            return COLOR_WIDTH;
        }

        public int getIconHeight() {
            return COLOR_HEIGHT + 2;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(BORDER_COLOR);
            g.drawRect(x + 2, y, COLOR_WIDTH, COLOR_HEIGHT);
            g.setColor(color);
            g.fillRect(x + 2 + 1, y + 1, COLOR_WIDTH - 1, COLOR_HEIGHT - 1);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        list = new javax.swing.JList();

        setLayout(new java.awt.BorderLayout());

        jScrollPane1.setBorder(null);
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list.setOpaque(false);
        jScrollPane1.setViewportView(list);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>                        
    // Variables declaration - do not modify                     
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList list;
    // End of variables declaration                   
}
