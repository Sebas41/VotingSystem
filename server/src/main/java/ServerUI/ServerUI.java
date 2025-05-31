package ServerUI;

import Controller.ServerControllerInterface;

import javax.swing.*;
import java.awt.*;

public class ServerUI extends JFrame implements ServerUIInterface {

    private static ServerUI instance;

    private final ServerControllerInterface controller;
    private final JTextArea voteLogArea;
    private final JTextArea electionInfoArea;
    private final JLabel statusLabel;

    public ServerUI(ServerControllerInterface controller) {
        this.controller = controller;
        setTitle("Servidor Central - Sistema de Votación");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        voteLogArea = new JTextArea();
        voteLogArea.setEditable(false);

        electionInfoArea = new JTextArea();
        electionInfoArea.setEditable(false);

        statusLabel = new JLabel("Estado: Inactivo");

        JButton btnInfo = new JButton("Mostrar Info Elección");
        btnInfo.addActionListener(e -> {
            String info = controller.getElectionInfo();
            showElectionInfo(info);
        });

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(btnInfo);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JScrollPane(voteLogArea), BorderLayout.CENTER);
        contentPanel.add(new JScrollPane(electionInfoArea), BorderLayout.EAST);
        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(statusLabel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
    }

    public static void launchUI(ServerControllerInterface controller) {
        instance = new ServerUI(controller);
        instance.setVisible(true);
    }

    public static ServerUI getInstance() {
        return instance;
    }

    @Override
    public void showVoteInfo(String voteInfo) {
        voteLogArea.append(voteInfo + "\n");
    }

    @Override
    public void showElectionInfo(String electionInfo) {
        electionInfoArea.setText(electionInfo);
    }

    @Override
    public void updateStatus(String status) {
        statusLabel.setText("Estado: " + status);
    }
}
