package concrete.goonie;


import concrete.goonie.ui.Fragment;

import javax.swing.*;
import java.awt.*;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        setLayout(new BorderLayout());
        add(new Chart(), SwingConstants.CENTER);

        JButton profileBtn = new JButton("Go to Profile");
        profileBtn.addActionListener(e ->
                navigationController.navigateTo("profile"));
        add(profileBtn, BorderLayout.SOUTH);
    }

    @Override
    public JComponent getView() {
        return this;
    }
    @Override
    public String getDestinationId() {
        return "home";
    }
}