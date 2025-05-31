package ui;

import java.util.Scanner;

/**
 * Vista de línea de comandos (CLI) para interactuar con el usuario.
 * Muestra menús, lee inputs y devuelve datos al controlador.
 */
public class View {
    private Scanner scanner;

    public View() {
        scanner = new Scanner(System.in);
    }

    /**
     * Muestra el prompt de login y devuelve un arreglo [id, password].
     */
    public String[] showLoginPrompt() {
        System.out.println("=== Sistema de Votación (CLI) ===");
        System.out.print("Cedula (ID): ");
        String id = scanner.nextLine().trim();
        System.out.print("Contraseña: ");
        String password = scanner.nextLine().trim();
        return new String[]{id, password};
    }

    /**
     * Muestra un mensaje de error en pantalla.
     */
    public void showError(String msg) {
        System.out.println("[ERROR] " + msg);
    }

    /**
     * Muestra un mensaje de información en pantalla.
     */
    public void showInfo(String msg) {
        System.out.println("[INFO] " + msg);
    }

    /**
     * Si el votante ya votó, se muestra este mensaje.
     */
    public void showAlreadyVoted() {
        System.out.println("Ya has ejercido tu voto. ¡Gracias!");
    }

    /**
     * Muestra el menú de candidatos fijos (A, B, C) y pide al usuario que elija.
     * Devuelve la opción (en mayúscula) si es válida: "A", "B" o "C".
     * Si se ingresa algo distinto, vuelve a preguntar.
     */
    public String showCandidatesAndGetChoice() {
        while (true) {
            System.out.println("\nSeleccione su candidato:");
            System.out.println("A) Candidato A");
            System.out.println("B) Candidato B");
            System.out.println("C) Candidato C");
            System.out.print("Opción [A/B/C]: ");
            String choice = scanner.nextLine().trim().toUpperCase();
            if (choice.equals("A") || choice.equals("B") || choice.equals("C")) {
                return choice;
            }
            System.out.println("Ingrese A, B o C. Intente de nuevo.");
        }
    }

    /**
     * Cierra el scanner (opcional) antes de terminar la aplicación.
     */
    public void close() {
        scanner.close();
    }
}
