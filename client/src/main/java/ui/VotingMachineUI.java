package ui;

import votation.Candidate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

public class VotingMachineUI extends JFrame implements VotingMachineUIinterface{

    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Panel de login
    private JPanel loginPanel;
    private JTextField idField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel loginMessage;

    // Panel de votación
    private JPanel votePanel;
    private JComboBox<Candidate> candidateCombo;
    private JButton voteButton;
    private JLabel voteMessage;

    public VotingMachineUI() {
        setTitle("Máquina de Votación");
        setSize(500, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        initLoginPanel();
        initVotePanel();

        mainPanel.add(loginPanel, "login");
        mainPanel.add(votePanel, "vote");

        add(mainPanel);
        showLoginPanel(); // Mostrar login al inicio
    }

    private void initLoginPanel() {
        loginPanel = new JPanel(new BorderLayout(10, 10));
        loginPanel.setBorder(BorderFactory.createTitledBorder("Inicio de Sesión"));

        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        idField = new JTextField();
        passwordField = new JPasswordField();

        fieldsPanel.add(new JLabel("Cédula:"));
        fieldsPanel.add(idField);
        fieldsPanel.add(new JLabel("Contraseña:"));
        fieldsPanel.add(passwordField);

        loginButton = new JButton("Iniciar Sesión");
        loginMessage = new JLabel(" ");
        loginMessage.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(loginButton, BorderLayout.CENTER);
        bottom.add(loginMessage, BorderLayout.SOUTH);

        loginPanel.add(fieldsPanel, BorderLayout.CENTER);
        loginPanel.add(bottom, BorderLayout.SOUTH);
    }

    private void initVotePanel() {
        votePanel = new JPanel(new BorderLayout(10, 10));
        votePanel.setBorder(BorderFactory.createTitledBorder("Emitir Voto"));

        candidateCombo = new JComboBox<>();
        voteButton = new JButton("Votar");
        voteMessage = new JLabel(" ");
        voteMessage.setHorizontalAlignment(SwingConstants.CENTER);

        votePanel.add(candidateCombo, BorderLayout.CENTER);
        votePanel.add(voteButton, BorderLayout.SOUTH);
        votePanel.add(voteMessage, BorderLayout.NORTH);
    }

    // Métodos de control

    public void showLoginPanel() {
        clearLoginFields();
        loginMessage.setText(" ");
        cardLayout.show(mainPanel, "login");
    }

    public void showVotePanel() {
        voteMessage.setText(" ");
        cardLayout.show(mainPanel, "vote");
    }

    public String getVoterId() {
        return idField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }


    @Override
    public void setCandidates(List<Candidate> candidates) {
        candidateCombo.removeAllItems();
        for (Candidate c : candidates) {
            candidateCombo.addItem(c);
        }
    }

    public String getSelectedCandidateId() {
        Candidate selected = (Candidate) candidateCombo.getSelectedItem();
        return selected != null ? String.valueOf(selected.getId()) : null;
    }

    @Override
    public void showLoginMessage(String msg, boolean isError) {
        loginMessage.setText(msg);
        loginMessage.setForeground(isError ? Color.RED : Color.BLUE);
    }


    @Override
    public void showVoteMessage(String msg, boolean isError) {
        voteMessage.setText(msg);
        voteMessage.setForeground(isError ? Color.RED : Color.BLUE);
    }

    public void addLoginAction(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void addVoteAction(ActionListener listener) {
        voteButton.addActionListener(listener);
    }


    @Override
    public void resetToLoginAfterVote() {
        // Llamado desde el controller cuando termina el voto
        JOptionPane.showMessageDialog(this, "Gracias por votar. Será redirigido a la pantalla de inicio.");
        showLoginPanel();
    }

    private void clearLoginFields() {
        idField.setText("");
        passwordField.setText("");
    }
}
