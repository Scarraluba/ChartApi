package concrete.goonie;

import concrete.goonie.ui.AppFrame;

import javax.swing.*;

public class Main extends AppFrame {
    public Main() {
        super("My Application");

        // Register fragments
        registerFragment(new HomeFragment());

        // Set initial state
        setRoot("home");
        setToolbarTitle("Home");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.setVisible(true);
        });
    }
}