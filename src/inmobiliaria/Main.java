package inmobiliaria;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import inmobiliaria.cobranza.servicio.MoraService;
import inmobiliaria.cobranza.servicio.PagoService;
import inmobiliaria.contratos.servicio.ContratoService;
import inmobiliaria.nucleo.historial.Historial;
import inmobiliaria.propiedades.servicio.PropiedadService;

public class Main {
    public static void main(String[] args) {
        Historial historial = new Historial();
        PropiedadService propiedades = new PropiedadService(historial);
        ContratoService contratos = new ContratoService(historial);
        PagoService pagos = new PagoService(historial);
        MoraService mora = new MoraService(pagos);

        try {
            propiedades.cargar();
            contratos.cargar();
            pagos.cargar();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null,
                "No se pudieron cargar los archivos de la carpeta data/:\n" + e,
                "Error al cargar", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() ->
            new MenuPrincipal(propiedades, contratos, pagos, mora).setVisible(true));
    }
}
