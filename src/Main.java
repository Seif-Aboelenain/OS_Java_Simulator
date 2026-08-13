import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Modern, flat cross-platform theme. Purely cosmetic — falls back
            // silently to the default look and feel if unavailable.
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
                // Keep the default look and feel.
            }

            new WelcomeScreen(() -> {
                MainGUI gui = new MainGUI();

                gui.setKernelFactory(() -> {
                    String choice = gui.getSelectedScheduler();
                    Scheduler scheduler;
                    switch (choice) {
                        case "HRRN":
                            scheduler = new HRRNScheduler();
                            break;
                        case "MLFQ":
                            scheduler = new MLFQScheduler();
                            break;
                        default:
                            scheduler = new RoundRobinScheduler();
                            break;
                    }
                    Kernel kernel = new Kernel(scheduler, gui);
                    gui.setKernel(kernel);//stores kernel inside gui(.)
                    kernel.startThread();//run simulation thread seperately(.)
                });
            });
        });
    }
}
