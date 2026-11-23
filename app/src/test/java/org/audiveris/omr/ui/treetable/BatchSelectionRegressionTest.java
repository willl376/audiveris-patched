//------------------------------------------------------------------------------------------------//
//                                                                                                //
//         B a t c h S e l e c t i o n R e g r e s s i o n T e s t                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// Regression tests for the 2026-11-22 Audiveris fixes:
//   - JTreeTable must not rebuild the whole tree selection for every
//     intermediate list-selection event of a batch.
//   - TextLine must tolerate a text line with no baseline (null de-skewed
//     origin) when sorting.
//------------------------------------------------------------------------------------------------//

package org.audiveris.omr.ui.treetable;

import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.geom.Point2D;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for the two Java defects fixed on 2026-11-22.
 */
public class BatchSelectionRegressionTest
{
    //~ Inner classes ------------------------------------------------------------------------------

    /**
     * Minimal tree-table model with a root and {@code n} children.
     */
    private static class Model
            extends AbstractTreeTableModel
    {
        Model (int n)
        {
            super(new DefaultMutableTreeNode("root"));

            for (int i = 0; i < n; i++) {
                ((DefaultMutableTreeNode) root).add(new DefaultMutableTreeNode("node" + i));
            }
        }

        @Override
        public int getColumnCount ()
        {
            return 1;
        }

        @Override
        public String getColumnName (int column)
        {
            return "Name";
        }

        @Override
        public Class<?> getColumnClass (int column)
        {
            return Object.class;
        }

        @Override
        public Object getValueAt (Object node, int column)
        {
            return node.toString();
        }

        @Override
        public boolean isCellEditable (Object node, int column)
        {
            return false;
        }

        @Override
        public void setValueAt (Object value, Object node, int column)
        {
        }

        @Override
        public Object getChild (Object parent, int index)
        {
            return ((TreeNode) parent).getChildAt(index);
        }

        @Override
        public int getChildCount (Object parent)
        {
            return ((TreeNode) parent).getChildCount();
        }

        @Override
        public boolean isLeaf (Object node)
        {
            return ((TreeNode) node).isLeaf();
        }

        @Override
        public int getIndexOfChild (Object parent, Object child)
        {
            return ((TreeNode) parent).getIndex((TreeNode) child);
        }

        @Override
        public void valueForPathChanged (TreePath path, Object newValue)
        {
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * A batch of row selections must emit a tree-selection event only once,
     * not once per row. The old behaviour was quadratic: 120 rows produced
     * thousands of events and could freeze the EDT for minutes.
     */
    @Test
    public void testBatchSelectionIsCoalesced ()
    {
        final int n = 120;

        int unbatched = countTreeEvents(n, false);
        int batched = countTreeEvents(n, true);

        assertTrue("Unbatched run should rebuild the tree per row, got " + unbatched,
                   unbatched > n * 2);
        assertTrue("Batched run should update the tree once, got " + batched,
                   batched <= n + 2);
    }

    /**
     * A line without a baseline has a null de-skewed origin. Sorting such
     * lines used to throw a NullPointerException in TextLine.byOrdinate().
     */
    @Test
    public void testNullOriginComparator () throws Exception
    {
        Class<?> textLineClass = Class.forName("org.audiveris.omr.text.TextLine");
        Method method = textLineClass.getDeclaredMethod(
                "compareOrigins", Point2D.class, Point2D.class, boolean.class);
        method.setAccessible(true);

        Point2D a = new Point2D.Double(10, 20);
        Point2D b = new Point2D.Double(30, 40);

        assertEquals(0, method.invoke(null, null, null, true));
        assertEquals(-1, method.invoke(null, null, a, true));
        assertEquals(1, method.invoke(null, a, null, true));
        assertEquals(0, method.invoke(null, a, a, false));
        assertEquals(Double.compare(10d, 30d), method.invoke(null, a, b, true));
        assertEquals(Double.compare(20d, 40d), method.invoke(null, a, b, false));
    }

    //~ Private methods ----------------------------------------------------------------------------

    /**
     * Select every row of a fresh table and count tree-selection events.
     *
     * @param n       number of child rows
     * @param batch   whether to mark the list selection as adjusting
     * @return number of tree-selection events fired during the batch
     */
    private static int countTreeEvents (int n, boolean batch)
    {
        JTreeTable table = new JTreeTable(new Model(n));
        table.getTree().expandRow(0);

        AtomicInteger events = new AtomicInteger();
        table.getTree().addTreeSelectionListener(e -> events.incrementAndGet());

        if (batch) {
            table.getSelectionModel().setValueIsAdjusting(true);
        }

        try {
            table.clearSelection();

            for (int i = 0; i < n; i++) {
                table.addRowSelectionInterval(i, i);
            }
        } finally {
            if (batch) {
                table.getSelectionModel().setValueIsAdjusting(false);
            }
        }

        return events.get();
    }
}
