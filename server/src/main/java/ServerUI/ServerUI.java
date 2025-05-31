package ServerUI;

import Controller.ServerControllerInterface;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
        setSize(800, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Componentes principales
        voteLogArea = new JTextArea();
        voteLogArea.setEditable(false);
        voteLogArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        voteLogArea.setBackground(new Color(245, 245, 245));
        voteLogArea.setBorder(BorderFactory.createTitledBorder("Historial de votos recibidos"));

        electionInfoArea = new JTextArea(8, 40);
        electionInfoArea.setEditable(false);
        electionInfoArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        electionInfoArea.setBorder(BorderFactory.createTitledBorder("Información de la elección"));

        statusLabel = new JLabel("Estado: Inactivo");
        statusLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Votos Recibidos", createVoteLogPanel());
        tabs.addTab("Gestión de Elección", createElectionPanel());
        tabs.addTab("Gestión de Candidatos", createCandidatePanel());
        tabs.addTab("Reportes", createReportsPanel());


        // Layout principal
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createVoteLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(new JScrollPane(voteLogArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createElectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(15);
        JTextField startField = new JTextField("30-05-2025 00:00", 15);
        JTextField endField = new JTextField("31-12-2025 23:59", 15);

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

        JButton btnInfo = new JButton("Mostrar Info");
        btnInfo.addActionListener(e -> showElectionInfo(controller.getElectionInfo()));

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("ID de Elección:"), gbc);
        gbc.gridx = 1; panel.add(idField, gbc);
        gbc.gridx = 0; gbc.gridy++; panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1; panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy++; panel.add(new JLabel("Inicio (dd-MM-yyyy HH:mm):"), gbc);
        gbc.gridx = 1; panel.add(startField, gbc);
        gbc.gridx = 0; gbc.gridy++; panel.add(new JLabel("Fin (dd-MM-yyyy HH:mm):"), gbc);
        gbc.gridx = 1; panel.add(endField, gbc);

        gbc.gridx = 0; gbc.gridy++; panel.add(btnCreate, gbc);
        gbc.gridx = 1; panel.add(btnInfo, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        panel.add(new JScrollPane(electionInfoArea), gbc);

        return panel;
    }

    private JPanel createCandidatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

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

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1; panel.add(idField, gbc);
        gbc.gridx = 0; gbc.gridy++; panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1; panel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy++; panel.add(new JLabel("Partido:"), gbc);
        gbc.gridx = 1; panel.add(partyField, gbc);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; panel.add(btnAdd, gbc);

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
        voteLogArea.append("" + voteInfo + "\n");
    }

    @Override
    public void showElectionInfo(String electionInfo) {
        electionInfoArea.setText(electionInfo);
    }

    @Override
    public void updateStatus(String status) {
        statusLabel.setText("Estado: " + status);
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField electionIdField = new JTextField("1", 10);
        JTextArea resultsArea = new JTextArea(12, 40);
        resultsArea.setEditable(false);
        resultsArea.setBorder(BorderFactory.createTitledBorder("Resultados"));
        JScrollPane scroll = new JScrollPane(resultsArea);

        // Mostrar votos por candidato
        JButton btnShowVotes = new JButton("Mostrar votos por candidato");
        btnShowVotes.addActionListener(e -> {
            try {
                int electionId = Integer.parseInt(electionIdField.getText().trim());
                String report = controller.getTotalVotesPerCandidate(electionId);
                resultsArea.setText(report);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        // Mostrar votos por candidato por máquina
        JButton btnShowByMachine = new JButton("Mostrar por máquina");
        btnShowByMachine.addActionListener(e -> {
            try {
                int electionId = Integer.parseInt(electionIdField.getText().trim());
                String report = controller.getVotesPerCandidateByMachine(electionId);
                resultsArea.setText(report);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        // Exportar por máquina
        JButton btnExportMachine = new JButton("Exportar por máquina CSV");
        btnExportMachine.addActionListener(e -> {
            try {
                int electionId = Integer.parseInt(electionIdField.getText().trim());
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String path = fileChooser.getSelectedFile().getAbsolutePath();
                    controller.exportVotesPerMachineCSV(electionId, path);
                    JOptionPane.showMessageDialog(this, "Reporte exportado a:\n" + path);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exportando: " + ex.getMessage());
            }
        });

        // Exportar resultados globales
        JButton btnExportGlobal = new JButton("Exportar resultados CSV");
        btnExportGlobal.addActionListener(e -> {
            try {
                int electionId = Integer.parseInt(electionIdField.getText().trim());
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String path = fileChooser.getSelectedFile().getAbsolutePath();
                    controller.exportElectionResultsCSV(electionId, path);
                    JOptionPane.showMessageDialog(this, "Resultados exportados a:\n" + path);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exportando: " + ex.getMessage());
            }
        });

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("ID Elección:"), gbc);
        gbc.gridx = 1; panel.add(electionIdField, gbc); row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(btnShowVotes, gbc); row++;
        panel.add(btnShowByMachine, gbc); row++;
        panel.add(btnExportMachine, gbc); row++;
        panel.add(btnExportGlobal, gbc); row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scroll, gbc);

        return panel;
    }




}
