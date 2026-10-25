package inmobiliaria;

import javax.swing.SwingUtilities;
import inmobiliaria.nucleo.estructuras.ListaSimple;
import inmobiliaria.nucleo.persistencia.GestorCSV;

/**
 * Punto de entrada. Arranca el menu Swing.
 *
 * Responsable: Integrante 1 - Francia Ramos Johan Mitziu
 */
public class Main {
    public static void main(String[] args) {
        // TODO: cargar CSV antes de mostrar el menu (2.8)
        ListaSimple<String> lineasPropietarios = new ListaSimple<>();
        GestorCSV.cargar("propietarios.csv", lineasPropietarios);

        SwingUtilities.invokeLater(() -> new MenuPrincipal().setVisible(true));
    }
}
