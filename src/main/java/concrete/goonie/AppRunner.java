package concrete.goonie;

import concrete.goonie.ui.AppFrame;

import javax.swing.*;

public class AppRunner extends AppFrame {
    public AppRunner() {
        super("My Application");

        // Register fragments
        registerFragment(new HomeFragment());

        // Set initial state
        setRoot("home");
        setToolbarTitle("Home");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppRunner app = new AppRunner();
            app.setVisible(true);
        });
    }
}