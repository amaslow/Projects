package desktopapplication1;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import org.jdesktop.beansbinding.Converter;


public class RowSorterToStringConverter extends Converter {

    private JTable table;

    public JTable getTable() {
        return table;
    }

    public void setTable(JTable table) {
        this.table = table;
    }

    @Override
    public Object convertForward(Object value) {
        return value.toString();
    }

    @Override
    public Object convertReverse(Object mask) {
        TableRowSorter sorter = new TableRowSorter(table.getModel());

        // The following statement makes the filter case-sensitive. If you want 
        //filter to work in a case-insensitive way, uncomment the line below, comment 
        //the 7 code lines below
        //sorter.setRowFilter(RowFilter.regexFilter(".\*" + mask + ".\*"));

        //The following 7 lines create a case-insensitive filter. If you want 
        //the filter to be case-sensitive, comment them out and uncomment the 
        //line above
        String m = mask.toString();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m.length(); i++) {
            char c = m.charAt(i);
            sb.append('[').append(Character.toLowerCase(c)).append(Character.toUpperCase(c)).append(']');
        }
         sorter.setRowFilter(RowFilter.regexFilter("(?i)" + sb + "(?i)"));
        return sorter;
    }
}