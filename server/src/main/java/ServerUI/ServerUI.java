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
 * 🏛️ INTERFAZ GRÁFICA MODERNIZADA DEL SERVIDOR ELECTORAL
 *
 * ✅ NUEVA: Usa ElectoralSystemController integrado
 * ✅ API MODERNA: Todos los métodos usan ElectionResult
 * ✅ MEJOR UX: Feedback visual mejorado y validación
 * ✅ FUNCIONALIDADES NUEVAS: Diagnósticos, monitoreo, reportes avanzados
 * ✅ RESPONSIVE: Diseño moderno y profesional
 */
public class ServerUI extends JFrame implements ServerUIInterface {

    private static ServerUI instance;
    private final ServerControllerImpl controller; // ✅ CAMBIO: Usar controller integrado

    // =================== COMPONENTES PRINCIPALES ===================
    private final JTextArea voteLogArea;
    private final JTextArea electionInfoArea;
    private final JTextArea systemLogArea;
    private final JLabel statusLabel;
    private final JLabel connectionStatusLabel;
    private final JProgressBar operationProgressBar;

    // =================== COLORES Y ESTILOS ===================
    private static final Color PRIMARY_COLOR = new Color(33, 150, 243);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color ERROR_COLOR = new Color(244, 67, 54);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);

    public ServerUI(ServerControllerImpl controller) {
        this.controller = controller;

        // =================== CONFIGURACIÓN VENTANA PRINCIPAL ===================
        setTitle("🏛️ Servidor Electoral - Sistema Integrado v2.0");
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(createIconImage());

        // =================== INICIALIZACIÓN DE COMPONENTES ===================
        voteLogArea = createStyledTextArea("Registro de votos en tiempo real");
        electionInfoArea = createStyledTextArea("Información detallada de la elección");
        systemLogArea = createStyledTextArea("Log del sistema y diagnósticos");

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

        // =================== LAYOUT PRINCIPAL ===================
        setLayout(new BorderLayout());

        // Panel superior con estado
        add(createHeaderPanel(), BorderLayout.NORTH);

        // Pestañas principales
        JTabbedPane mainTabs = createMainTabPane();
        add(mainTabs, BorderLayout.CENTER);

        // Panel inferior con estado
        add(createFooterPanel(), BorderLayout.SOUTH);

        // =================== INICIALIZACIÓN AUTOMÁTICA ===================
        initializeSystemStatus();
    }

    // =================== CREACIÓN DE COMPONENTES ===================

    private JTextArea createStyledTextArea(String tooltip) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setBackground(BACKGROUND_COLOR);
        area.setToolTipText(tooltip);
        area.setMargin(new Insets(10, 10, 10, 10));
        return area;
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_COLOR);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("🏛️ Sistema Electoral Integrado");
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

        // ✅ PESTAÑAS PRINCIPALES
        tabs.addTab("📊 Dashboard", createDashboardPanel());
        tabs.addTab("🗳️ Gestión de Elecciones", createElectionPanel());
        tabs.addTab("👥 Gestión de Candidatos", createCandidatePanel());
        tabs.addTab("📤 Configuración de Mesas", createConfigurationPanel());
        tabs.addTab("📈 Reportes y Consultas", createReportsPanel());
        tabs.addTab("🔧 Monitoreo del Sistema", createMonitoringPanel());
        tabs.addTab("📋 Registro de Votos", createVoteLogPanel());

        return tabs;
    }

    // =================== 📊 PANEL DASHBOARD ===================

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de métricas principales
        JPanel metricsPanel = new JPanel(new GridLayout(2, 3, 15, 15));

        metricsPanel.add(createMetricCard("🗳️ Estado Elección", "Cargando...", SUCCESS_COLOR));
        metricsPanel.add(createMetricCard("👥 Total Candidatos", "Cargando...", PRIMARY_COLOR));
        metricsPanel.add(createMetricCard("📊 Votos Registrados", "Cargando...", WARNING_COLOR));
        metricsPanel.add(createMetricCard("🔌 Estado BD", "Cargando...", SUCCESS_COLOR));
        metricsPanel.add(createMetricCard("📡 Conexiones", "Cargando...", PRIMARY_COLOR));
        metricsPanel.add(createMetricCard("⚡ Rendimiento", "Cargando...", SUCCESS_COLOR));

        // Panel de acciones rápidas
        JPanel actionsPanel = createQuickActionsPanel();

        // Panel de información del sistema
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(createTitledBorder("📋 Información del Sistema"));
        infoPanel.add(new JScrollPane(systemLogArea));

        // Layout del dashboard
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(metricsPanel, BorderLayout.CENTER);
        topPanel.add(actionsPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMetricCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                new EmptyBorder(15, 15, 15, 15)
        ));
        card.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(color);

        JLabel valueLabel = new JLabel(value, JLabel.CENTER);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createQuickActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(createTitledBorder("⚡ Acciones Rápidas"));

        JButton btnRefresh = createStyledButton("🔄 Actualizar Estado", PRIMARY_COLOR);
        btnRefresh.addActionListener(e -> refreshSystemStatus());

        JButton btnDiagnostic = createStyledButton("🔍 Ejecutar Diagnóstico", WARNING_COLOR);
        btnDiagnostic.addActionListener(e -> runSystemDiagnostic());

        JButton btnStartVoting = createStyledButton("🗳️ Iniciar Votación", SUCCESS_COLOR);
        btnStartVoting.addActionListener(e -> startVoting());

        JButton btnStopVoting = createStyledButton("🔒 Detener Votación", ERROR_COLOR);
        btnStopVoting.addActionListener(e -> stopVoting());

        panel.add(btnRefresh);
        panel.add(btnDiagnostic);
        panel.add(btnStartVoting);
        panel.add(btnStopVoting);

        return panel;
    }

    // =================== 🗳️ PANEL GESTIÓN DE ELECCIONES ===================

    private JPanel createElectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de creación de elección
        JPanel creationPanel = new JPanel(new GridBagLayout());
        creationPanel.setBorder(createTitledBorder("➕ Crear Nueva Elección"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ✅ CAMBIO: Ya no necesita ID, se genera automáticamente
        JTextField nameField = new JTextField(20);
        JTextField startField = new JTextField("15-06-2025 00:00", 15);
        JTextField endField = new JTextField("15-06-2025 23:59", 15);

        JButton btnCreate = createStyledButton("✅ Crear Elección", SUCCESS_COLOR);
        btnCreate.addActionListener(e -> createElection(nameField, startField, endField));

        JButton btnInfo = createStyledButton("📋 Mostrar Información", PRIMARY_COLOR);
        btnInfo.addActionListener(e -> showElectionInfo());

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; creationPanel.add(new JLabel("📝 Nombre de la Elección:"), gbc);
        gbc.gridx = 1; creationPanel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy++; creationPanel.add(new JLabel("📅 Inicio (dd-MM-yyyy HH:mm):"), gbc);
        gbc.gridx = 1; creationPanel.add(startField, gbc);
        gbc.gridx = 0; gbc.gridy++; creationPanel.add(new JLabel("📅 Fin (dd-MM-yyyy HH:mm):"), gbc);
        gbc.gridx = 1; creationPanel.add(endField, gbc);

        gbc.gridx = 0; gbc.gridy++; creationPanel.add(btnCreate, gbc);
        gbc.gridx = 1; creationPanel.add(btnInfo, gbc);

        // Panel de información
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(createTitledBorder("📊 Información de la Elección"));
        infoPanel.add(new JScrollPane(electionInfoArea));

        panel.add(creationPanel, BorderLayout.NORTH);
        panel.add(infoPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 👥 PANEL GESTIÓN DE CANDIDATOS ===================

    private JPanel createCandidatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de registro de candidatos
        JPanel registrationPanel = new JPanel(new GridBagLayout());
        registrationPanel.setBorder(createTitledBorder("➕ Registrar Nuevo Candidato"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField electionIdField = new JTextField("1", 10);
        JTextField nameField = new JTextField(20);
        JTextField partyField = new JTextField(20);

        JButton btnAdd = createStyledButton("✅ Registrar Candidato", SUCCESS_COLOR);
        btnAdd.addActionListener(e -> addCandidate(electionIdField, nameField, partyField));

        JButton btnLoad = createStyledButton("📄 Cargar desde CSV", WARNING_COLOR);
        btnLoad.addActionListener(e -> loadCandidatesFromCSV(electionIdField));

        JButton btnList = createStyledButton("📋 Listar Candidatos", PRIMARY_COLOR);
        btnList.addActionListener(e -> listCandidates(electionIdField));

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; registrationPanel.add(new JLabel("🗳️ ID Elección:"), gbc);
        gbc.gridx = 1; registrationPanel.add(electionIdField, gbc);
        gbc.gridx = 0; gbc.gridy++; registrationPanel.add(new JLabel("👤 Nombre:"), gbc);
        gbc.gridx = 1; registrationPanel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy++; registrationPanel.add(new JLabel("🏛️ Partido:"), gbc);
        gbc.gridx = 1; registrationPanel.add(partyField, gbc);

        gbc.gridx = 0; gbc.gridy++; registrationPanel.add(btnAdd, gbc);
        gbc.gridx = 1; registrationPanel.add(btnLoad, gbc);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; registrationPanel.add(btnList, gbc);

        // Panel de lista de candidatos
        JTextArea candidatesArea = createStyledTextArea("Lista de candidatos registrados");
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(createTitledBorder("👥 Candidatos Registrados"));
        listPanel.add(new JScrollPane(candidatesArea));

        panel.add(registrationPanel, BorderLayout.NORTH);
        panel.add(listPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 📤 PANEL CONFIGURACIÓN DE MESAS ===================

    private JPanel createConfigurationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de configuración
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(createTitledBorder("📤 Envío de Configuraciones"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField mesaIdField = new JTextField("6823", 10);
        JTextField electionIdField = new JTextField("1", 10);
        JTextField departmentIdField = new JTextField("1", 10);

        JButton btnSendMesa = createStyledButton("📤 Enviar a Mesa", PRIMARY_COLOR);
        btnSendMesa.addActionListener(e -> sendConfigurationToMesa(mesaIdField, electionIdField));

        JButton btnSendDepartment = createStyledButton("🏛️ Enviar a Departamento", WARNING_COLOR);
        btnSendDepartment.addActionListener(e -> sendConfigurationToDepartment(departmentIdField, electionIdField));

        JButton btnCheckStatus = createStyledButton("🔍 Verificar Estado", SUCCESS_COLOR);
        btnCheckStatus.addActionListener(e -> checkMesaStatus(mesaIdField));

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; configPanel.add(new JLabel("📋 ID Mesa:"), gbc);
        gbc.gridx = 1; configPanel.add(mesaIdField, gbc);
        gbc.gridx = 0; gbc.gridy++; configPanel.add(new JLabel("🗳️ ID Elección:"), gbc);
        gbc.gridx = 1; configPanel.add(electionIdField, gbc);
        gbc.gridx = 0; gbc.gridy++; configPanel.add(new JLabel("🏛️ ID Departamento:"), gbc);
        gbc.gridx = 1; configPanel.add(departmentIdField, gbc);

        gbc.gridx = 0; gbc.gridy++; configPanel.add(btnSendMesa, gbc);
        gbc.gridx = 1; configPanel.add(btnSendDepartment, gbc);
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; configPanel.add(btnCheckStatus, gbc);

        // Panel de resultados
        JTextArea resultsArea = createStyledTextArea("Resultados de configuración");
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
        queryPanel.setBorder(createTitledBorder("🔍 Consultas y Reportes"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField documentField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JTextField lastNameField = new JTextField(15);
        JTextField electionIdField = new JTextField("1", 10);
        JTextField departmentIdField = new JTextField("1", 10);

        // Botones de consulta
        JButton btnCitizenReport = createStyledButton("📋 Reporte Ciudadano", PRIMARY_COLOR);
        btnCitizenReport.addActionListener(e -> generateCitizenReport(documentField, electionIdField));

        JButton btnSearchCitizen = createStyledButton("🔍 Buscar Ciudadanos", SUCCESS_COLOR);
        btnSearchCitizen.addActionListener(e -> searchCitizens(nameField, lastNameField));

        JButton btnElectionResults = createStyledButton("📊 Resultados Elección", WARNING_COLOR);
        btnElectionResults.addActionListener(e -> getElectionResults(electionIdField));

        JButton btnDepartmentReport = createStyledButton("🏛️ Reporte Departamento", ERROR_COLOR);
        btnDepartmentReport.addActionListener(e -> getDepartmentReport(departmentIdField, electionIdField));

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; queryPanel.add(new JLabel("🆔 Documento:"), gbc);
        gbc.gridx = 1; queryPanel.add(documentField, gbc);
        gbc.gridx = 0; gbc.gridy++; queryPanel.add(new JLabel("👤 Nombre:"), gbc);
        gbc.gridx = 1; queryPanel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy++; queryPanel.add(new JLabel("👤 Apellido:"), gbc);
        gbc.gridx = 1; queryPanel.add(lastNameField, gbc);
        gbc.gridx = 0; gbc.gridy++; queryPanel.add(new JLabel("🗳️ ID Elección:"), gbc);
        gbc.gridx = 1; queryPanel.add(electionIdField, gbc);
        gbc.gridx = 0; gbc.gridy++; queryPanel.add(new JLabel("🏛️ ID Departamento:"), gbc);
        gbc.gridx = 1; queryPanel.add(departmentIdField, gbc);

        gbc.gridx = 0; gbc.gridy++; queryPanel.add(btnCitizenReport, gbc);
        gbc.gridx = 1; queryPanel.add(btnSearchCitizen, gbc);
        gbc.gridx = 0; gbc.gridy++; queryPanel.add(btnElectionResults, gbc);
        gbc.gridx = 1; queryPanel.add(btnDepartmentReport, gbc);

        // Panel de resultados
        JTextArea resultsArea = createStyledTextArea("Resultados de consultas y reportes");
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

        // Panel de acciones de monitoreo
        JPanel actionsPanel = new JPanel(new FlowLayout());
        actionsPanel.setBorder(createTitledBorder("🔧 Herramientas de Monitoreo"));

        JButton btnSystemStatus = createStyledButton("📊 Estado del Sistema", PRIMARY_COLOR);
        btnSystemStatus.addActionListener(e -> showSystemStatus());

        JButton btnDiagnostic = createStyledButton("🔍 Diagnóstico Completo", WARNING_COLOR);
        btnDiagnostic.addActionListener(e -> runFullDiagnostic());

        JButton btnPerformance = createStyledButton("⚡ Estadísticas de Rendimiento", SUCCESS_COLOR);
        btnPerformance.addActionListener(e -> showPerformanceStats());

        JButton btnRefresh = createStyledButton("🔄 Refrescar Todo", ERROR_COLOR);
        btnRefresh.addActionListener(e -> refreshAllData());

        actionsPanel.add(btnSystemStatus);
        actionsPanel.add(btnDiagnostic);
        actionsPanel.add(btnPerformance);
        actionsPanel.add(btnRefresh);

        // Panel de monitoreo
        JTextArea monitoringArea = createStyledTextArea("Información de monitoreo del sistema");
        JPanel monitoringPanel = new JPanel(new BorderLayout());
        monitoringPanel.setBorder(createTitledBorder("📈 Monitor del Sistema"));
        monitoringPanel.add(new JScrollPane(monitoringArea));

        panel.add(actionsPanel, BorderLayout.NORTH);
        panel.add(monitoringPanel, BorderLayout.CENTER);

        return panel;
    }

    // =================== 📋 PANEL REGISTRO DE VOTOS ===================

    private JPanel createVoteLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel de control
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(createTitledBorder("⚙️ Control del Registro"));

        JButton btnClear = createStyledButton("🗑️ Limpiar Log", ERROR_COLOR);
        btnClear.addActionListener(e -> voteLogArea.setText(""));

        JButton btnSave = createStyledButton("💾 Guardar Log", SUCCESS_COLOR);
        btnSave.addActionListener(e -> saveVoteLog());

        controlPanel.add(btnClear);
        controlPanel.add(btnSave);

        // Panel de log
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(createTitledBorder("📋 Registro de Votos Recibidos"));
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

    private Image createIconImage() {
        // Crear un ícono simple para la aplicación
        return Toolkit.getDefaultToolkit().createImage(new byte[0]);
    }

    // =================== 🔧 MÉTODOS DE ACCIÓN ===================

    private void createElection(JTextField nameField, JTextField startField, JTextField endField) {
        showProgress("Creando elección...");

        CompletableFuture.runAsync(() -> {
            try {
                String name = nameField.getText().trim();
                Date start = new SimpleDateFormat("dd-MM-yyyy HH:mm").parse(startField.getText().trim());
                Date end = new SimpleDateFormat("dd-MM-yyyy HH:mm").parse(endField.getText().trim());

                // ✅ CAMBIO: Usar nueva API sin ID
                ElectionResult result = controller.createElection(name, start, end);

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    if (result.isSuccess()) {
                        showSuccess("✅ " + result.getMessage());
                        nameField.setText("");
                        showElectionInfo();
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

    private void addCandidate(JTextField electionIdField, JTextField nameField, JTextField partyField) {
        showProgress("Registrando candidato...");

        CompletableFuture.runAsync(() -> {
            try {
                int electionId = Integer.parseInt(electionIdField.getText().trim());
                String name = nameField.getText().trim();
                String party = partyField.getText().trim();

                // ✅ CAMBIO: Usar nueva API
                ElectionResult result = controller.addCandidate(electionId, name, party);

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    if (result.isSuccess()) {
                        showSuccess("✅ " + result.getMessage());
                        nameField.setText("");
                        partyField.setText("");
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
                // ✅ CAMBIO: Usar nueva API para elección 1 por defecto
                ElectionResult result = controller.getElectionInfo(1);

                SwingUtilities.invokeLater(() -> {
                    if (result.isSuccess()) {
                        electionInfoArea.setText(formatElectionInfo(result));
                    } else {
                        electionInfoArea.setText("❌ " + result.getMessage());
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    electionInfoArea.setText("❌ Error: " + ex.getMessage());
                });
            }
        });
    }

    private void startVoting() {
        showProgress("Iniciando votación...");

        CompletableFuture.runAsync(() -> {
            try {
                // ✅ CAMBIO: Usar nueva API
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
                // ✅ CAMBIO: Usar nueva API
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

    private void runSystemDiagnostic() {
        showProgress("Ejecutando diagnóstico del sistema...");

        CompletableFuture.runAsync(() -> {
            try {
                // ✅ NUEVO: Usar diagnóstico integrado
                ElectionResult result = controller.runSystemDiagnostic();

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    systemLogArea.setText(formatDiagnosticResult(result));
                    if (result.isSuccess()) {
                        showSuccess("🔍 Diagnóstico completado");
                    } else {
                        showError("❌ " + result.getMessage());
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("❌ Error en diagnóstico: " + ex.getMessage());
                });
            }
        });
    }

    // =================== 🔧 MÉTODOS HELPER ===================

    private void initializeSystemStatus() {
        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult status = controller.getSystemStatus();

                SwingUtilities.invokeLater(() -> {
                    if (status.isSuccess()) {
                        systemLogArea.append("✅ Sistema inicializado correctamente\n");
                        systemLogArea.append("📊 Estado: " + status.getMessage() + "\n");
                        connectionStatusLabel.setText("🟢 Sistema Listo");
                        connectionStatusLabel.setForeground(SUCCESS_COLOR);
                    } else {
                        systemLogArea.append("⚠️ Problemas durante inicialización\n");
                        connectionStatusLabel.setText("🟡 Con Problemas");
                        connectionStatusLabel.setForeground(WARNING_COLOR);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    systemLogArea.append("❌ Error obteniendo estado inicial: " + e.getMessage() + "\n");
                    connectionStatusLabel.setText("🔴 Error");
                    connectionStatusLabel.setForeground(ERROR_COLOR);
                });
            }
        });
    }

    private void refreshSystemStatus() {
        showProgress("Actualizando estado del sistema...");

        CompletableFuture.runAsync(() -> {
            try {
                ElectionResult status = controller.getSystemStatus();

                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    systemLogArea.append("🔄 [" + new Date() + "] Estado actualizado\n");
                    if (status.isSuccess()) {
                        systemLogArea.append("✅ Sistema operando normalmente\n");
                        showSuccess("Sistema actualizado correctamente");
                    } else {
                        systemLogArea.append("⚠️ " + status.getMessage() + "\n");
                        showWarning("Sistema con problemas detectados");
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    hideProgress();
                    showError("Error actualizando estado: " + e.getMessage());
                });
            }
        });
    }

    private String formatElectionInfo(ElectionResult result) {
        if (!result.isSuccess()) {
            return "❌ " + result.getMessage();
        }

        Map<String, Object> data = result.getData();
        StringBuilder info = new StringBuilder();

        info.append("🗳️ ========== INFORMACIÓN DE LA ELECCIÓN ==========\n");
        info.append("📊 Estado: ").append(result.getMessage()).append("\n");
        info.append("📅 Consultado: ").append(result.getTimestamp()).append("\n\n");

        if (data.containsKey("nombre")) {
            info.append("📝 Nombre: ").append(data.get("nombre")).append("\n");
        }
        if (data.containsKey("estado")) {
            info.append("🎯 Estado: ").append(data.get("estado")).append("\n");
        }
        if (data.containsKey("candidateCount")) {
            info.append("👥 Candidatos: ").append(data.get("candidateCount")).append("\n");
        }

        info.append("================================================\n");

        return info.toString();
    }

    private String formatDiagnosticResult(ElectionResult result) {
        StringBuilder diagnostic = new StringBuilder();

        diagnostic.append("🔍 ========== DIAGNÓSTICO DEL SISTEMA ==========\n");
        diagnostic.append("📅 Fecha: ").append(new Date()).append("\n");
        diagnostic.append("📊 Estado: ").append(result.isSuccess() ? "✅ EXITOSO" : "❌ CON PROBLEMAS").append("\n");
        diagnostic.append("💬 Mensaje: ").append(result.getMessage()).append("\n\n");

        if (result.isSuccess() && result.getData() != null) {
            Map<String, Object> data = result.getData();

            // Información de la base de datos
            if (data.containsKey("database")) {
                diagnostic.append("💾 Base de Datos:\n");
                @SuppressWarnings("unchecked")
                Map<String, Object> dbInfo = (Map<String, Object>) data.get("database");
                diagnostic.append("   - Estado: ").append(dbInfo.get("healthy")).append("\n");
            }

            // Resumen del diagnóstico
            if (data.containsKey("summary")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> summary = (Map<String, Object>) data.get("summary");
                diagnostic.append("\n📋 Resumen:\n");
                diagnostic.append("   - Problemas encontrados: ").append(summary.get("issuesFound")).append("\n");
                diagnostic.append("   - Estado general: ").append(summary.get("overallHealth")).append("\n");
            }
        }

        diagnostic.append("================================================\n");

        return diagnostic.toString();
    }

    // =================== 🎨 MÉTODOS DE UI ===================

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
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showWarning(String message) {
        statusLabel.setText("🟡 " + message);
        statusLabel.setForeground(WARNING_COLOR);
    }

    // =================== 📋 MÉTODOS DE INTERFAZ LEGACY ===================

    @Override
    public void showVoteInfo(String voteInfo) {
        voteLogArea.append("🗳️ [" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + voteInfo + "\n");
        voteLogArea.setCaretPosition(voteLogArea.getDocument().getLength());
    }

    @Override
    public void showElectionInfo(String electionInfo) {
        electionInfoArea.setText(electionInfo);
    }

    @Override
    public void updateStatus(String status) {
        statusLabel.setText("📊 " + status);
    }

    // =================== 🚀 MÉTODOS ESTÁTICOS ===================

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

    // =================== 🔧 MÉTODOS ADICIONALES (PLACEHOLDER) ===================

    private void loadCandidatesFromCSV(JTextField electionIdField) {
        // TODO: Implementar carga de CSV
        showWarning("Funcionalidad en desarrollo");
    }

    private void listCandidates(JTextField electionIdField) {
        // TODO: Implementar listado de candidatos
        showWarning("Funcionalidad en desarrollo");
    }

    private void sendConfigurationToMesa(JTextField mesaIdField, JTextField electionIdField) {
        // TODO: Implementar envío a mesa
        showWarning("Funcionalidad en desarrollo");
    }

    private void sendConfigurationToDepartment(JTextField departmentIdField, JTextField electionIdField) {
        // TODO: Implementar envío a departamento
        showWarning("Funcionalidad en desarrollo");
    }

    private void checkMesaStatus(JTextField mesaIdField) {
        // TODO: Implementar verificación de estado
        showWarning("Funcionalidad en desarrollo");
    }

    private void generateCitizenReport(JTextField documentField, JTextField electionIdField) {
        // TODO: Implementar reporte de ciudadano
        showWarning("Funcionalidad en desarrollo");
    }

    private void searchCitizens(JTextField nameField, JTextField lastNameField) {
        // TODO: Implementar búsqueda de ciudadanos
        showWarning("Funcionalidad en desarrollo");
    }

    private void getElectionResults(JTextField electionIdField) {
        // TODO: Implementar resultados de elección
        showWarning("Funcionalidad en desarrollo");
    }

    private void getDepartmentReport(JTextField departmentIdField, JTextField electionIdField) {
        // TODO: Implementar reporte de departamento
        showWarning("Funcionalidad en desarrollo");
    }

    private void showSystemStatus() {
        refreshSystemStatus();
    }

    private void runFullDiagnostic() {
        runSystemDiagnostic();
    }

    private void showPerformanceStats() {
        // TODO: Implementar estadísticas de rendimiento
        showWarning("Funcionalidad en desarrollo");
    }

    private void refreshAllData() {
        refreshSystemStatus();
        showElectionInfo();
    }

    private void saveVoteLog() {
        // TODO: Implementar guardado de log
        showWarning("Funcionalidad en desarrollo");
    }
}