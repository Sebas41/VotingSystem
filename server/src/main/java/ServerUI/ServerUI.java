package ServerUI;

import Controller.ServerControllerInterface;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerUI extends JFrame implements ServerUIInterface {

    private static ServerUI instance;

    private final ServerControllerInterface controller;
    private final JTextArea voteLogArea;
    private final JTextArea electionInfoArea;
    private final JLabel statusLabel;

    public ServerUI(ServerControllerInterface controller) {
        this.controller = controller;
        setTitle("Servidor Central - Sistema de Votación");
        setSize(700, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Áreas de texto
        voteLogArea = new JTextArea();
        voteLogArea.setEditable(false);

        electionInfoArea = new JTextArea();
        electionInfoArea.setEditable(false);

        statusLabel = new JLabel("Estado: Inactivo");

        // Pestañas
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Votos Recibidos", createVoteLogPanel());
        tabs.addTab("Gestión de Elección", createElectionPanel());
        tabs.addTab("Gestión de Candidatos", createCandidatePanel());

        // Layout
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createVoteLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(voteLogArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createElectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(15);
        JTextField startField = new JTextField("dd-MM-yyyy HH:mm", 15);
        JTextField endField = new JTextField("dd-MM-yyyy HH:mm", 15);

        JButton btnCreate = new JButton("Crear Elección");
        btnCreate.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String name = nameField.getText().trim();
                Date start = new SimpleDateFormat("dd-MM-yyyy HH:mm").parse(startField.getText().trim());
                Date end = new SimpleDateFormat("dd-MM-yyyy HH:mm").parse(endField.getText().trim());

                controller.createElection(id, name, start, end);
                updateStatus("Elección creada");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al crear la elección: " + ex.getMessage());
            }
        });

        JButton btnInfo = new JButton("Mostrar Info Elección");
        btnInfo.addActionListener(e -> {
            String info = controller.getElectionInfo();
            showElectionInfo(info);
        });

        panel.add(new JLabel("ID de Elección:")); panel.add(idField);
        panel.add(new JLabel("Nombre:")); panel.add(nameField);
        panel.add(new JLabel("Inicio (dd-MM-yyyy HH:mm):")); panel.add(startField);
        panel.add(new JLabel("Fin (dd-MM-yyyy HH:mm):")); panel.add(endField);
        panel.add(btnCreate);
        panel.add(btnInfo);
        panel.add(new JScrollPane(electionInfoArea));

        return panel;
    }

    private JPanel createCandidatePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(15);
        JTextField partyField = new JTextField(15);

        JButton btnAdd = new JButton("Registrar Candidato");
        btnAdd.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String name = nameField.getText().trim();
                String party = partyField.getText().trim();

                controller.addCandidate(id, name, party);
                updateStatus("Candidato añadido: " + name);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al registrar candidato: " + ex.getMessage());
            }
        });

        panel.add(new JLabel("ID:")); panel.add(idField);
        panel.add(new JLabel("Nombre:")); panel.add(nameField);
        panel.add(new JLabel("Partido:")); panel.add(partyField);
        panel.add(btnAdd);

        return panel;
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
