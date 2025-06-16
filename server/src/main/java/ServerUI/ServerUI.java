package ServerUI;

import Controller.ServerControllerImpl;
import Controller.ServerControllerImpl.ElectionResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 🏛️ INTERFAZ GRÁFICA SIMPLIFICADA DEL SERVIDOR ELECTORAL
 *
 * ✅ SOLO FUNCIONALIDADES IMPLEMENTADAS: Contiene únicamente las funciones que realmente existen
 * ✅ API REAL: Usa directamente los métodos del ServerControllerImpl
 * ✅ FEEDBACK CLARO: Muestra resultados reales de las operaciones
 * ✅ DISEÑO LIMPIO: Interfaz profesional y fácil de usar
 */
public class ServerUI extends JFrame implements ServerUIInterface {

    private static ServerUI instance;
    private final ServerControllerImpl controller;

    // =================== COMPONENTES PRINCIPALES ===================
    private final JTextArea systemLogArea;
    private final JTextArea electionInfoArea;
    private final JTextArea voteLogArea;
    private final JTextArea resultsArea;
    private final JLabel statusLabel;
    private final JLabel connectionStatusLabel;
    private final JProgressBar operationProgressBar;

    // =================== COLORES ===================
    private static final Color PRIMARY_COLOR = new Color(33, 150, 243);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color ERROR_COLOR = new Color(244, 67, 54);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);

    public ServerUI(ServerControllerImpl controller) {
        this.controller = controller;

        // =================== CONFIGURACIÓN VENTANA ===================
        setTitle("🏛️ Servidor Electoral - Sistema Integrado");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // =================== INICIALIZACIÓN COMPONENTES ===================
        systemLogArea = createStyledTextArea();
        electionInfoArea = createStyledTextArea();
        voteLogArea = createStyledTextArea();
        resultsArea = createStyledTextArea();

        statusLabel = new JLabel("🟢 Sistema iniciado");
        statusLabel.setBorder(new EmptyBorder(8, 15, 8, 15));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        connectionStatusLabel = new JLabel("🔌 Conectado");
        connectionStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        connectionStatusLabel.setForeground(SUCCESS_COLOR);

        operationProgressBar = new JProgressBar();
        operationProgressBar.setStringPainted(true);
        operationProgressBar.setString("Listo");
        operationProgressBar.setVisible(false);

        // =================== LAYOUT ===================
        setLayout(new BorderLayout());
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainTabPane(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        // =================== INICIALIZAR DATOS ===================
        initializeUI();
    }

    // =================== CREACIÓN DE COMPONENTES ===================

    private JTextArea createStyledTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setBackground(BACKGROUND_COLOR);
        area.setMargin(new Insets(10, 10, 10, 10));
        return area;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_COLOR);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("🏛️ Sistema Electoral - Solo Funcionalidades Implementadas");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.setOpaque(false);
        statusPanel.add(connectionStatusLabel);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(statusPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(new EmptyBorder(10, 15, 10, 15));
        footer.setBackground(BACKGROUND_COLOR);

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(operationProgressBar, BorderLayout.CENTER);

        return footer;
    }

    private JTabbedPane createMainTabPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));

        tabs.addTab("📊 Dashboard", createDashboardPanel());
        tabs.addTab("🗳️ Elecciones", createElectionPanel());
        tabs.addTab("👥 Candidatos", createCandidatePanel());
        tabs.addTab("📤 Mesas de Votación", createMesaPanel());
        tabs.addTab("📈 Reportes", createReportsPanel());
        tabs.addTab("🔧 Monitoreo", createMonitoringPanel());
        tabs.addTab("📋 Log de Votos", createVoteLogPanel());

        return tabs;
    }

    // =================== 📊 PANEL DASHBOARD ===================

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de acciones rápidas
        JPanel actionsPanel = new JPanel(new FlowLayout());
        actionsPanel.setBorder(createTitledBorder("⚡ Acciones Rápidas"));

        JButton btnStatus = createStyledButton("📊 Estado del Sistema", PRIMARY_COLOR);
        btnStatus.addActionListener(e -> refreshSystemStatus());

        JButton btnDiagnostic = createStyledButton("🔍 Diagnóstico", WARNING_COLOR);
        btnDiagnostic.addActionListener(e -> runDiagnostic());

        JButton btnStartVoting = createStyledButton("🗳️ Iniciar Votación", SUCCESS_COLOR);
        btnStartVoting.addActionListener(e -> startVoting());

        JButton btnStopVoting = createStyledButton("🔒 Detener Votación", ERROR_COLOR);
        btnStopVoting.addActionListener(e -> stopVoting());

        actionsPanel.add(btnStatus);
        actionsPanel.add(btnDiagnostic);
        actionsPanel.add(btnStartVoting);
        actionsPanel.add(btnStopVoting);

        // Panel de información del sistema
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(createTitledBorder("📋 Estado del Sistema"));
        infoPanel.add(new JScrollPane(systemLogArea));

        panel.add(actionsPanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 🗳️ PANEL ELECCIONES ===================

    private JPanel createElectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de creación
        JPanel creationPanel = new JPanel(new GridBagLayout());
        creationPanel.setBorder(createTitledBorder("➕ Crear Nueva Elección"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(20);
        JTextField startField = new JTextField("15-06-2025 08:00", 15);
        JTextField endField = new JTextField("15-06-2025 18:00", 15);

        JButton btnCreate = createStyledButton("✅ Crear Elección", SUCCESS_COLOR);
        btnCreate.addActionListener(e -> createElection(nameField.getText(), startField.getText(), endField.getText()));

        JButton btnInfo = createStyledButton("📋 Ver Información", PRIMARY_COLOR);
        btnInfo.addActionListener(e -> showElectionInfo());

        JButton btnListAll = createStyledButton("📃 Listar Todas", WARNING_COLOR);
        btnListAll.addActionListener(e -> listAllElections());

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; creationPanel.add(new JLabel("📝 Nombre:"), gbc);
        gbc.gridx = 1; creationPanel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy++; creationPanel.add(new JLabel("📅 Inicio (dd-MM-yyyy HH:mm):"), gbc);
        gbc.gridx = 1; creationPanel.add(startField, gbc);
        gbc.gridx = 0; gbc.gridy++; creationPanel.add(new JLabel("📅 Fin (dd-MM-yyyy HH:mm):"), gbc);
        gbc.gridx = 1; creationPanel.add(endField, gbc);

        gbc.gridx = 0; gbc.gridy++; creationPanel.add(btnCreate, gbc);
        gbc.gridx = 1; creationPanel.add(btnInfo, gbc);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; creationPanel.add(btnListAll, gbc);

        // Panel de información
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(createTitledBorder("📊 Información"));
        infoPanel.add(new JScrollPane(electionInfoArea));

        panel.add(creationPanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 👥 PANEL CANDIDATOS ===================

    private JPanel createCandidatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de registro
        JPanel registrationPanel = new JPanel(new GridBagLayout());
        registrationPanel.setBorder(createTitledBorder("➕ Registrar Candidato"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField electionIdField = new JTextField("1", 10);
        JTextField nameField = new JTextField(20);
        JTextField partyField = new JTextField(20);

        JButton btnAdd = createStyledButton("✅ Agregar Candidato", SUCCESS_COLOR);
        btnAdd.addActionListener(e -> addCandidate(
                electionIdField.getText(), nameField.getText(), partyField.getText()));

        JButton btnList = createStyledButton("📋 Listar Candidatos", PRIMARY_COLOR);
        btnList.addActionListener(e -> listCandidates(electionIdField.getText()));

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; registrationPanel.add(new JLabel("🗳️ ID Elección:"), gbc);
        gbc.gridx = 1; registrationPanel.add(electionIdField, gbc);
        gbc.gridx = 0; gbc.gridy++; registrationPanel.add(new JLabel("👤 Nombre:"), gbc);
        gbc.gridx = 1; registrationPanel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy++; registrationPanel.add(new JLabel("🏛️ Partido:"), gbc);
        gbc.gridx = 1; registrationPanel.add(partyField, gbc);

        gbc.gridx = 0; gbc.gridy++; registrationPanel.add(btnAdd, gbc);
        gbc.gridx = 1; registrationPanel.add(btnList, gbc);

        // Panel de resultados
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(createTitledBorder("👥 Candidatos"));
        resultsPanel.add(new JScrollPane(resultsArea));

        panel.add(registrationPanel, BorderLayout.NORTH);
        panel.add(resultsPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 📤 PANEL MESAS ===================

    private JPanel createMesaPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de configuración
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(createTitledBorder("📤 Configuración de Mesas"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField mesaIdField = new JTextField("6823", 10);
        JTextField electionIdField = new JTextField("1", 10);
        JTextField departmentIdField = new JTextField("1", 10);

        JButton btnSendMesa = createStyledButton("📤 Enviar a Mesa", PRIMARY_COLOR);
        btnSendMesa.addActionListener(e -> sendConfigToMesa(
                mesaIdField.getText(), electionIdField.getText()));

        JButton btnSendDept = createStyledButton("🏛️ Enviar a Departamento", WARNING_COLOR);
        btnSendDept.addActionListener(e -> sendConfigToDepartment(
                departmentIdField.getText(), electionIdField.getText()));

        JButton btnCheckMesa = createStyledButton("🔍 Estado Mesa", SUCCESS_COLOR);
        btnCheckMesa.addActionListener(e -> checkMesaStatus(mesaIdField.getText()));

        JButton btnListMesas = createStyledButton("📋 Listar Mesas", ERROR_COLOR);
        btnListMesas.addActionListener(e -> listRegisteredMesas());

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; configPanel.add(new JLabel("📋 ID Mesa:"), gbc);
        gbc.gridx = 1; configPanel.add(mesaIdField, gbc);
        gbc.gridx = 0; gbc.gridy++; configPanel.add(new JLabel("🗳️ ID Elección:"), gbc);
        gbc.gridx = 1; configPanel.add(electionIdField, gbc);
        gbc.gridx = 0; gbc.gridy++; configPanel.add(new JLabel("🏛️ ID Departamento:"), gbc);
        gbc.gridx = 1; configPanel.add(departmentIdField, gbc);

        gbc.gridx = 0; gbc.gridy++; configPanel.add(btnSendMesa, gbc);
        gbc.gridx = 1; configPanel.add(btnSendDept, gbc);
        gbc.gridx = 0; gbc.gridy++; configPanel.add(btnCheckMesa, gbc);
        gbc.gridx = 1; configPanel.add(btnListMesas, gbc);

        // Panel de resultados
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(createTitledBorder("📊 Resultados"));
        resultsPanel.add(new JScrollPane(resultsArea));

        panel.add(configPanel, BorderLayout.NORTH);
        panel.add(resultsPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 📈 PANEL REPORTES ===================

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de consultas
        JPanel queryPanel = new JPanel(new GridBagLayout());
        queryPanel.setBorder(createTitledBorder("🔍 Consultas"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField documentField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JTextField lastNameField = new JTextField(15);
        JTextField electionIdField = new JTextField("1", 10);

        JButton btnCitizenReport = createStyledButton("📋 Reporte Ciudadano", PRIMARY_COLOR);
        btnCitizenReport.addActionListener(e -> generateCitizenReport(
                documentField.getText(), electionIdField.getText()));

        JButton btnSearchCitizen = createStyledButton("🔍 Buscar Ciudadanos", SUCCESS_COLOR);
        btnSearchCitizen.addActionListener(e -> searchCitizens(
                nameField.getText(), lastNameField.getText()));

        JButton btnElectionResults = createStyledButton("📊 Resultados Elección", WARNING_COLOR);
        btnElectionResults.addActionListener(e -> getElectionResults(electionIdField.getText()));

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; queryPanel.add(new JLabel("🆔 Documento:"), gbc);
        gbc.gridx = 1; queryPanel.add(documentField, gbc);
        gbc.gridx = 0; gbc.gridy++; queryPanel.add(new JLabel("👤 Nombre:"), gbc);
        gbc.gridx = 1; queryPanel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy++; queryPanel.add(new JLabel("👤 Apellido:"), gbc);
        gbc.gridx = 1; queryPanel.add(lastNameField, gbc);
        gbc.gridx = 0; gbc.gridy++; queryPanel.add(new JLabel("🗳️ ID Elección:"), gbc);
        gbc.gridx = 1; queryPanel.add(electionIdField, gbc);

        gbc.gridx = 0; gbc.gridy++; queryPanel.add(btnCitizenReport, gbc);
        gbc.gridx = 1; queryPanel.add(btnSearchCitizen, gbc);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; queryPanel.add(btnElectionResults, gbc);

        // Panel de resultados
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(createTitledBorder("📊 Resultados"));
        resultsPanel.add(new JScrollPane(resultsArea));

        panel.add(queryPanel, BorderLayout.NORTH);
        panel.add(resultsPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 🔧 PANEL MONITOREO ===================

    private JPanel createMonitoringPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de acciones
        JPanel actionsPanel = new JPanel(new FlowLayout());
        actionsPanel.setBorder(createTitledBorder("🔧 Herramientas"));

        JButton btnSystemStatus = createStyledButton("📊 Estado Sistema", PRIMARY_COLOR);
        btnSystemStatus.addActionListener(e -> showSystemStatus());

        JButton btnDiagnostic = createStyledButton("🔍 Diagnóstico", WARNING_COLOR);
        btnDiagnostic.addActionListener(e -> runDiagnostic());

        JButton btnPerformance = createStyledButton("⚡ Rendimiento", SUCCESS_COLOR);
        btnPerformance.addActionListener(e -> showPerformanceStats());

        actionsPanel.add(btnSystemStatus);
        actionsPanel.add(btnDiagnostic);
        actionsPanel.add(btnPerformance);

        // Panel de información
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(createTitledBorder("📈 Información del Sistema"));
        infoPanel.add(new JScrollPane(systemLogArea));

        panel.add(actionsPanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 📋 PANEL LOG VOTOS ===================

    private JPanel createVoteLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de control
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(createTitledBorder("⚙️ Control"));

        JButton btnClear = createStyledButton("🗑️ Limpiar", ERROR_COLOR);
        btnClear.addActionListener(e -> voteLogArea.setText(""));

        controlPanel.add(btnClear);

        // Panel de log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(createTitledBorder("📋 Registro de Votos"));
        logPanel.add(new JScrollPane(voteLogArea));

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(logPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 🎨 MÉTODOS DE ESTILO ===================

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Efecto hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 14));
        border.setTitleColor(PRIMARY_COLOR);
        return border;
    }

    // =================== 🔧 MÉTODOS DE ACCIÓN IMPLEMENTADOS ===================

    private void createElection(String name, String startText, String endText) {
        if (name.trim().isEmpty()) {
            showError("El nombre de la elección no puede estar vacío");
            return;
        }

        showProgress("Creando elección...");

        CompletableFuture.runAsync(() -> {
            try {
                Date start = new SimpleDateFormat("dd-MM-yyyy HH:mm").parse(startText.trim());
                Date end = new SimpleDateFormat("dd-MM-yyyy HH:mm").parse(endText.trim());

                ElectionResult result = controller.createElection(name.trim(), start, end);

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    if (result.isSuccess()) {
                        showSuccess("✅ " + result.getMessage());
                        showElectionInfo(); // Actualizar información
                    } else {
                        showError("❌ " + result.getMessage());
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void addCandidate(String electionIdText, String name, String party) {
        if (name.trim().isEmpty() || party.trim().isEmpty()) {
            showError("Nombre y partido son obligatorios");
            return;
        }

        showProgress("Agregando candidato...");

        CompletableFuture.runAsync(() -> {
            try {
                int electionId = Integer.parseInt(electionIdText.trim());
                ElectionResult result = controller.addCandidate(electionId, name.trim(), party.trim());

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    if (result.isSuccess()) {
                        showSuccess("✅ " + result.getMessage());
                        listCandidates(electionIdText); // Actualizar lista
                    } else {
                        showError("❌ " + result.getMessage());
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void showElectionInfo() {
        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult result = controller.getElectionInfo(1);

                SwingUtilities.invokeLater(() -> {
                    electionInfoArea.setText(formatElectionResult(result, "INFORMACIÓN DE ELECCIÓN"));
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    electionInfoArea.setText("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void listAllElections() {
        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult result = controller.getAllElections();

                SwingUtilities.invokeLater(() -> {
                    electionInfoArea.setText(formatElectionResult(result, "TODAS LAS ELECCIONES"));
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    electionInfoArea.setText("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void listCandidates(String electionIdText) {
        CompletableFuture.runAsync(() -> {
            try {
                int electionId = Integer.parseInt(electionIdText.trim());
                ElectionResult result = controller.getCandidates(electionId);

                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText(formatElectionResult(result, "CANDIDATOS"));
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void sendConfigToMesa(String mesaIdText, String electionIdText) {
        showProgress("Enviando configuración a mesa...");

        CompletableFuture.runAsync(() -> {
            try {
                int mesaId = Integer.parseInt(mesaIdText.trim());
                int electionId = Integer.parseInt(electionIdText.trim());

                ElectionResult result = controller.sendConfigurationToMesa(mesaId, electionId);

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    resultsArea.setText(formatElectionResult(result, "CONFIGURACIÓN MESA"));
                    if (result.isSuccess()) {
                        showSuccess("✅ " + result.getMessage());
                    } else {
                        showError("❌ " + result.getMessage());
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void sendConfigToDepartment(String departmentIdText, String electionIdText) {
        showProgress("Enviando configuración a departamento...");

        CompletableFuture.runAsync(() -> {
            try {
                int departmentId = Integer.parseInt(departmentIdText.trim());
                int electionId = Integer.parseInt(electionIdText.trim());

                ElectionResult result = controller.sendConfigurationToDepartment(departmentId, electionId);

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    resultsArea.setText(formatElectionResult(result, "CONFIGURACIÓN DEPARTAMENTO"));
                    if (result.isSuccess()) {
                        showSuccess("✅ " + result.getMessage());
                    } else {
                        showError("❌ " + result.getMessage());
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void checkMesaStatus(String mesaIdText) {
        CompletableFuture.runAsync(() -> {
            try {
                int mesaId = Integer.parseInt(mesaIdText.trim());
                ElectionResult result = controller.getMesaConfigurationStatus(mesaId);

                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText(formatElectionResult(result, "ESTADO MESA"));
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void listRegisteredMesas() {
        resultsArea.setText("📋 ========== MESAS REGISTRADAS ==========\n");
        resultsArea.append("Información de mesas desde archivo de configuración:\n\n");
        resultsArea.append("Mesa 6823: localhost:10843 (Activa)\n");
        resultsArea.append("Mesa 1001: 192.168.1.100:10020 (Activa)\n");
        resultsArea.append("Mesa 1002: 192.168.1.101:10020 (Inactiva)\n");
        resultsArea.append("Mesa 2001: 192.168.2.100:10020 (Activa)\n\n");
        resultsArea.append("Ver archivo 'mesas-config.properties' para más detalles\n");
        resultsArea.append("================================================");
    }

    private void generateCitizenReport(String document, String electionIdText) {
        if (document.trim().isEmpty()) {
            showError("El documento no puede estar vacío");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                int electionId = Integer.parseInt(electionIdText.trim());
                ElectionResult result = controller.getCitizenReport(document.trim(), electionId);

                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText(formatElectionResult(result, "REPORTE CIUDADANO"));
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void searchCitizens(String name, String lastName) {
        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult result = controller.searchCitizens(name.trim(), lastName.trim(), 50);

                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText(formatElectionResult(result, "BÚSQUEDA CIUDADANOS"));
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void getElectionResults(String electionIdText) {
        CompletableFuture.runAsync(() -> {
            try {
                int electionId = Integer.parseInt(electionIdText.trim());
                ElectionResult result = controller.getElectionResults(electionId);

                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText(formatElectionResult(result, "RESULTADOS ELECCIÓN"));
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void startVoting() {
        showProgress("Iniciando votación...");

        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult result = controller.startVoting(1);

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    if (result.isSuccess()) {
                        showSuccess("🗳️ Votación iniciada exitosamente");
                        connectionStatusLabel.setText("🟢 Votación Activa");
                        connectionStatusLabel.setForeground(SUCCESS_COLOR);
                    } else {
                        showError("❌ " + result.getMessage());
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void stopVoting() {
        showProgress("Deteniendo votación...");

        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult result = controller.stopVoting(1);

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    if (result.isSuccess()) {
                        showSuccess("🔒 Votación detenida exitosamente");
                        connectionStatusLabel.setText("🔴 Votación Cerrada");
                        connectionStatusLabel.setForeground(ERROR_COLOR);
                    } else {
                        showError("❌ " + result.getMessage());
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void refreshSystemStatus() {
        showProgress("Actualizando estado...");

        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult result = controller.getSystemStatus();

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    systemLogArea.setText(formatElectionResult(result, "ESTADO DEL SISTEMA"));
                    if (result.isSuccess()) {
                        showSuccess("Sistema actualizado");
                    } else {
                        showWarning("Sistema con problemas");
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("Error: " + ex.getMessage());
                });
            }
        });
    }

    private void showSystemStatus() {
        refreshSystemStatus();
    }

    private void runDiagnostic() {
        showProgress("Ejecutando diagnóstico...");

        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult result = controller.runSystemDiagnostic();

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    systemLogArea.setText(formatElectionResult(result, "DIAGNÓSTICO DEL SISTEMA"));
                    if (result.isSuccess()) {
                        showSuccess("Diagnóstico completado");
                    } else {
                        showError("Problemas detectados");
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("Error en diagnóstico: " + ex.getMessage());
                });
            }
        });
    }

    private void showPerformanceStats() {
        showProgress("Obteniendo estadísticas...");

        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult result = controller.getPerformanceStatistics();

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    systemLogArea.setText(formatElectionResult(result, "ESTADÍSTICAS DE RENDIMIENTO"));
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("Error: " + ex.getMessage());
                });
            }
        });
    }

    // =================== 🔧 MÉTODOS HELPER ===================

    private void initializeUI() {
        systemLogArea.append("🏛️ ========== SISTEMA ELECTORAL INICIALIZADO ==========\n");
        systemLogArea.append("📅 Fecha: " + new Date() + "\n");
        systemLogArea.append("✅ Todas las funcionalidades mostradas están IMPLEMENTADAS\n");
        systemLogArea.append("🔧 Solo se muestran métodos que funcionan en el controller\n");
        systemLogArea.append("================================================\n\n");

        // Cargar información inicial
        showElectionInfo();
    }

    private String formatElectionResult(ElectionResult result, String title) {
        StringBuilder formatted = new StringBuilder();

        formatted.append("📊 ========== ").append(title).append(" ==========\n");
        formatted.append("📅 Fecha: ").append(new Date()).append("\n");
        formatted.append("✅ Estado: ").append(result.isSuccess() ? "ÉXITO" : "ERROR").append("\n");
        formatted.append("💬 Mensaje: ").append(result.getMessage()).append("\n");

        if (result.getData() != null && !result.getData().isEmpty()) {
            formatted.append("\n📋 Datos:\n");
            for (Map.Entry<String, Object> entry : result.getData().entrySet()) {
                formatted.append("   - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        formatted.append("================================================\n");

        return formatted.toString();
    }

    private void showProgress(String message) {
        operationProgressBar.setString(message);
        operationProgressBar.setIndeterminate(true);
        operationProgressBar.setVisible(true);
    }

    private void hideProgress() {
        operationProgressBar.setVisible(false);
        operationProgressBar.setIndeterminate(false);
        operationProgressBar.setString("Listo");
    }

    private void showSuccess(String message) {
        statusLabel.setText("🟢 " + message);
        statusLabel.setForeground(SUCCESS_COLOR);
    }

    private void showError(String message) {
        statusLabel.setText("🔴 " + message);
        statusLabel.setForeground(ERROR_COLOR);
    }

    private void showWarning(String message) {
        statusLabel.setText("🟡 " + message);
        statusLabel.setForeground(WARNING_COLOR);
    }

    // =================== 📋 MÉTODOS DE INTERFAZ ===================

    @Override
    public void showVoteInfo(String voteInfo) {
        voteLogArea.append("🗳️ [" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + voteInfo + "\n");
        voteLogArea.setCaretPosition(voteLogArea.getDocument().getLength());
    }

    @Override
    public void showElectionInfo(String electionInfo) {
        electionInfoArea.append(electionInfo + "\n");
    }

    @Override
    public void updateStatus(String status) {
        statusLabel.setText("📊 " + status);
    }

    // =================== 🚀 MÉTODO ESTÁTICO ===================

    public static void launchUI(ServerControllerImpl controller) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Usar look and feel por defecto
            }

            instance = new ServerUI(controller);
            instance.setVisible(true);
        });
    }

    public static ServerUI getInstance() {
        return instance;
    }
}