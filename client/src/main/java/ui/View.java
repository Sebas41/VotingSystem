package ui;

import votation.Candidate;

import java.util.List;
import java.util.Scanner;

public class View {
    private Scanner scanner;

    public View() {
        scanner = new Scanner(System.in);
    }

    public String[] showLoginPrompt() {
        System.out.println("=== Sistema de Votación (CLI) ===");
        System.out.print("Cedula (ID): ");
        String id = scanner.nextLine().trim();
        System.out.print("Contraseña: ");
        String password = scanner.nextLine().trim();
        return new String[]{id, password};
    }

    public void showError(String msg) {
        System.out.println("[ERROR] " + msg);
    }

    public void showInfo(String msg) {
        System.out.println("[INFO] " + msg);
    }

    public void showAlreadyVoted() {
        System.out.println("Ya has ejercido tu voto. ¡Gracias!");
    }

    public String showCandidatesAndGetChoice(List<Candidate> candidates) {
        System.out.println("=== Candidatos Disponibles ===");
        for (Candidate c : candidates) {
            System.out.println("ID: " + c.getId() + " - " + c.getName() + " (" + c.getParty() + ")");
        }
        System.out.print("Ingrese el ID del candidato por el que desea votar: ");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine().trim();  // Retorna el ID como string
    }

    public void close() {
        scanner.close();
    }
}
