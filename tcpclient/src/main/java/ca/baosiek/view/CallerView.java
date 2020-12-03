package ca.baosiek.view;

import ca.baosiek.controller.CallerController;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CallerView extends JFrame {

    private static final long serialVersionUID = 1L;
    private final String name;
    private FlowLayout flowLayout;
    private JTextArea logArea;

    public CallerView(String name, CallerController controller) {

        this.name = name;

        // Define JFrane Layout manager
        flowLayout = new FlowLayout();
        this.setLayout(flowLayout);

        // Create main panel and set its layout manager
        JPanel main = new JPanel();
        main.setLayout(new BorderLayout(5, 5));

        // North Panel
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new FlowLayout());
        JLabel callerNameLable = new JLabel("Caller Name:");
        JTextField callerName = new JTextField(this.name);
        callerName.setEditable(false);

        controller.setGraceTime(Duration.ofSeconds(Long.valueOf(0)));

        JLabel graceTimeLabel = new JLabel("Grace Time(s):");
        JTextField graceTime = new JTextField("0");
        graceTime.setToolTipText("Press ENTER to register new grace time interval");
        graceTime.setPreferredSize(new Dimension(50, 20));

        JButton gtb = new JButton("Set");

        // Action is to first validate graceTime value greater than zero
        // And second do set in the appropriate methods in this view controller
        gtb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                String s = e.getActionCommand();
                if (s.equals("Set")) {
                    // Print text in log area.
                    if (Long.valueOf(graceTime.getText()) <= 0) {
                        logArea.append(String.format(
                                "Grace time value needs to be a positive integer to be set. Value is set to: %s\n",
                                graceTime.getText()));
                        return;
                    }
                    controller.setGraceTime(Duration.ofSeconds(Long.valueOf(graceTime.getText())));
                    controller.setGraceTimeFlag(true);
                }
            }

        });

        // Displays the clock
        JLabel timeLabel = new JLabel("Time:");
        JTextField time = new JTextField();
        this.clock(time);

        // Populate panel
        String tab = new String("           ");
        northPanel.add(callerNameLable);
        northPanel.add(callerName);
        northPanel.add(new JLabel(tab));
        northPanel.add(graceTimeLabel);
        northPanel.add(graceTime);
        northPanel.add(gtb);
        northPanel.add(new JLabel(tab));
        northPanel.add(timeLabel);
        northPanel.add(time);

        // Create JTable to display services
        JTable table = new JTable(controller.getTableModel());

        // TableModelListner is managed by the controller
        table.getModel().addTableModelListener(controller);

        // Adjust table column sizes and justification
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(400);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setPreferredWidth(400);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setPreferredWidth(250);
        table.getColumnModel().getColumn(5).setPreferredWidth(200);
        table.getColumnModel().getColumn(6).setPreferredWidth(400);
        table.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(7).setPreferredWidth(200);
        table.getColumnModel().getColumn(8).setPreferredWidth(250);
        table.getColumnModel().getColumn(8).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(9).setPreferredWidth(800);
        table.getColumnModel().getColumn(9).setCellRenderer(centerRenderer);

        // Populate center panel with the table
        JPanel centerPanel = new JPanel();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(1300, 400));
        centerPanel.add(scrollPane);

        // South panel is just a log area to displaying messages
        JPanel southPanel = new JPanel();
        logArea = new JTextArea();
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setCaretPosition(logArea.getDocument().getLength());
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setPreferredSize(new Dimension(1300, 200));
        southPanel.add(scrollLog);

        // Populate main panel
        main.add(northPanel, BorderLayout.NORTH);
        main.add(centerPanel, BorderLayout.CENTER);
        main.add(southPanel, BorderLayout.SOUTH);
        add(main);
    }

    public void clock(JTextField time) {

        Thread clock = new Thread() {

            public void run() {

                while (true) {
                    try {
                        LocalDateTime ft = LocalDateTime.parse(LocalDateTime.now().toString(),
                                DateTimeFormatter.ISO_DATE_TIME);
                        time.setText(localDateTime2String(ft));
                        sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        };

        clock.start();
    }

    // Helper method
    public String localDateTime2String(LocalDateTime ldt) {

        LocalDateTime ft = LocalDateTime.parse(ldt.toString(), DateTimeFormatter.ISO_DATE_TIME);
        return String.format("%02d/%02d/%4d %02d:%02d:%02d", ft.getDayOfMonth(), ft.getMonthValue(), ft.getYear(),
                ft.getHour(), ft.getMinute(), ft.getSecond());

    }

    public void log(String message) {

        this.logArea.append(message);
    }

}
