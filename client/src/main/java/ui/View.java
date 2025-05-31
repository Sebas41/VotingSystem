package ui;

import votation.Candidate;

import java.util.List;
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
     * Muestra dinámicamente los candidatos y permite seleccionar uno por ID.
     *
     * @param candidates Lista de candidatos cargados desde el archivo JSON.
     * @return El nombre del candidato seleccionado (puede cambiarse a ID si lo prefieres).
     */
    public String showCandidatesAndGetChoice(List<Candidate> candidates) {
        while (true) {
            System.out.println("\nSeleccione su candidato:");
            for (Candidate c : candidates) {
                System.out.println(c.getId() + ") " + c.getName() + " - " + c.getPoliticalParty());
            }
            System.out.print("Ingrese el ID del candidato: ");
            String choice = scanner.nextLine().trim();

            try {
                int selectedId = Integer.parseInt(choice);
                for (Candidate c : candidates) {
                    if (c.getId() == selectedId) {
                        return String.valueOf(selectedId); // O puedes retornar `c.getName()` si prefieres
                    }
                }
                System.out.println("ID no encontrado. Intente de nuevo.");
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Ingrese un número.");
            }
        }
    }

    /**
     * Cierra el scanner (opcional) antes de terminar la aplicación.
     */
    public void close() {
        scanner.close();
    }
}
