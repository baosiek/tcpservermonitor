package ca.baosiek.model;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class TableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private final String[] columnNames;
    private final Class<?>[] classNames;
    private Vector<ServiceModel> tableData;

    public TableModel(Vector<ServiceModel> data) {

        this.columnNames = new String[10];
        this.columnNames[0] = "Hostname";
        this.columnNames[1] = "Port";
        this.columnNames[2] = "Outage Start";
        this.columnNames[3] = "Outage Stop";
        this.columnNames[4] = "Poll Frequency";
        this.columnNames[5] = "SetOutage";
        this.columnNames[6] = "Last Update";
        this.columnNames[7] = "Registered";
        this.columnNames[8] = "Status";
        this.columnNames[9] = "ServiceName";

        this.classNames = new Class[10];
        this.classNames[0] = String.class;
        this.classNames[1] = Integer.class;
        this.classNames[2] = String.class;
        this.classNames[3] = String.class;
        this.classNames[4] = Long.class;
        this.classNames[5] = Boolean.class;
        this.classNames[6] = String.class;
        this.classNames[7] = Boolean.class;
        this.classNames[8] = String.class;
        this.classNames[9] = String.class;

        this.tableData = data;
    }

    @Override
    public boolean isCellEditable(int row, int column) {

        switch (column) {

            case 2:
                return true;

            case 3:
                return true;

            case 4:
                return true;

            case 5:
                return true;

            case 7:
                return true;

            default:
                return false;

        }
    }

    @Override
    public String getColumnName(int index) {

        return this.columnNames[index];
    }

    @Override
    public Class<?> getColumnClass(int index) {

        return this.classNames[index];
    }

    @Override
    public int getRowCount() {

        return this.tableData.size();
    }

    @Override
    public int getColumnCount() {

        return this.columnNames.length;
    }

    @Override
    public Object getValueAt(int row, int col) {

        ServiceModel line = this.tableData.get(row);

        switch (col) {

            case 0:
                return line.getHostname();

            case 1:
                return line.getPort();

            case 2:
                LocalDateTime start = line.getStart();
                return localDateTime2String(start);

            case 3:
                LocalDateTime stop = line.getStop();
                return localDateTime2String(stop);

            case 4:
                return line.getPoll();

            case 5:
                return line.getIsOutage();

            case 6:
                LocalDateTime now = line.getLastUpdate();
                return localDateTime2String(now);

            case 7:
                return line.getIsRegistered();

            case 8:
                return line.getStatus();

            case 9:
                return line.getServiceName();

            default:
                return new Object();
        }
    }

    @Override
    public void setValueAt(Object obj, int row, int col) {

        ServiceModel line = this.tableData.get(row);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        switch (col) {

            case 0:
                line.setHostname((String) obj);
                break;

            case 1:
                line.setPort((Integer) obj);
                break;

            case 2:
                String start = (String) obj;
                line.setStart(LocalDateTime.parse(start, formatter));
                fireTableCellUpdated(row, col);
                break;

            case 3:
                String stop = (String) obj;
                line.setStop(LocalDateTime.parse(stop, formatter));
                fireTableCellUpdated(row, col);
                break;

            case 4:
                line.setPoll((Long) obj);
                fireTableCellUpdated(row, col);
                break;

            case 5:
                line.setIsOutage((Boolean) obj);
                fireTableCellUpdated(row, col);
                break;

            case 6:
                String now = (String) obj;
                line.setLastUpdate(LocalDateTime.parse(now, DateTimeFormatter.ISO_DATE_TIME));
                fireTableCellUpdated(row, col);
                break;

            case 7:
                line.setIsRegistered((Boolean) obj);
                fireTableCellUpdated(row, col);
                break;

            case 8:
                line.setStatus((String) obj);
                fireTableCellUpdated(row, col);
                break;

            case 9:
                line.setServiceName((String) obj);
                fireTableCellUpdated(row, col);
        }
    }

    public String localDateTime2String(LocalDateTime ldt) {

        LocalDateTime ft = LocalDateTime.parse(ldt.toString(), DateTimeFormatter.ISO_DATE_TIME);
        return String.format("%02d/%02d/%4d %02d:%02d:%02d", ft.getDayOfMonth(), ft.getMonthValue(), ft.getYear(),
                ft.getHour(), ft.getMinute(), ft.getSecond());
    }

}
