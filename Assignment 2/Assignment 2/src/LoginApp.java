import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(Enclosed.class)
public class LoginApp extends JFrame {
    private JTextField emailField;
    private JPasswordField passwordField;

    public static final String DB_URL = "jdbc:mysql://localhost:3306/softwaretesting";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234UIop[";

    public LoginApp() {
        setTitle("Login Screen");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2, 10, 10));

        // Email Label and Text Field
        panel.add(new JLabel("Email:"));
        emailField = new JTextField();
        panel.add(emailField);

        // Password Label and Password Field
        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        // Login Button
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new LoginAction());
        panel.add(loginButton);

        add(panel);
    }

    private class LoginAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Email and password cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String userName = null;
            try {
                userName = authenticateUser(email, password,DB_URL);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            if (userName != null) {
                JOptionPane.showMessageDialog(null, "Welcome, " + userName + "!", "Login Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Invalid email or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static String authenticateUser(String email, String password, String url) throws Exception {
        String userName = null;
        try (Connection conn = DriverManager.getConnection(url, DB_USER, DB_PASSWORD)) {
            String query = "SELECT name FROM User WHERE email = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                userName = rs.getString("name");
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return userName;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginApp loginApp = new LoginApp();
            loginApp.setVisible(true);
        });
    }

    public static class LoginAppTest{

        @Test
        public void testValidEmailAndPassword() throws Exception {
            String userName = authenticateUser("123@123.com", "123456",DB_URL);
            assertNotNull(userName, "Expected user to be authenticated.");
            assertEquals("hello", userName, "User name should match.");
        }

        @Test
        public void testInvalidEmail() throws Exception {
            String userName = authenticateUser("invalid@example.com", "password123", DB_URL);
            assertNull(userName, "Expected no user to be authenticated for an invalid email.");
        }

        @Test
        public void testEmptyFields() throws Exception {
            String userName = authenticateUser("", "",DB_URL);
            assertNull(userName, "Expected no user to be authenticated with empty fields.");
        }

        @Test
        public void testSQLInjectionAttempt() throws Exception {
            String userName = authenticateUser("'; DROP TABLE User; --", "password123", DB_URL);
            assertNull(userName, "Expected no user to be authenticated for SQL injection attempts.");
        }

        @Test
        public void testDatabaseConnectionFailure() {

            Exception exception = assertThrows(Exception.class, () -> {
                authenticateUser("test@example.com", "password123","error");
            });
            assertNotNull(exception.getMessage());
        }
    }

}
