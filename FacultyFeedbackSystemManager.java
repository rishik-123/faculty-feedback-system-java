/*
Compile Command:
javac -cp ".;mysql-connector-java-5.1.48.jar;jfreechart-1.0.19.jar;jcommon-1.0.23.jar" FacultyFeedbackSystemManager.java

Run Command:
java -cp ".;mysql-connector-java-5.1.48.jar;jfreechart-1.0.19.jar;jcommon-1.0.23.jar" FacultyFeedbackSystemManager
*/

import java.sql.*;
import java.util.Scanner;
import javax.swing.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;   
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import java.awt.Color;
import java.io.*;

// ====================== PASSWORD MASK ======================
class PasswordMask {
    public static String readPassword(String prompt) {
        System.out.print(prompt);
        StringBuilder password = new StringBuilder();
        try {
            while (true) {
                int chInt = System.in.read();
                if (chInt == -1) break;
                char ch = (char) chInt;
                if (ch == '\r' || ch == '\n') {
                    System.out.println();
                    break;
                } else {
                    password.append(ch);
                    System.out.print('*');
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return password.toString();
    }
}

// ====================== CUSTOM EXCEPTION ======================
class InvalidUsernameException extends Exception {
    public String toString() {
        return "Invalid Username! Must include at least one special symbol (e.g. $, _, @).";
    }
}

// ====================== ABSTRACT CLASS ======================
abstract class User {
    String username;
    String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    abstract void login();
}

// ====================== STUDENT CLASS ======================
class Student extends User {
    Connection con;
    int rollNo, semester;
    String name, department;

    Student(String username, String password) {
        super(username, password);
        try {
            con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/feedback_system?useSSL=false&allowPublicKeyRetrieval=true",
                "root", "rishik@12345"
            );
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e);
        }
    }

    // ---------- SIGNUP ----------
    public void signUp() {
        Scanner sc = FacultyFeedbackSystemManager.sc;
        try {
            System.out.print("Enter name: ");
            name = sc.nextLine();

            System.out.print("Enter department: ");
            department = sc.nextLine();

            System.out.print("Enter roll number: ");
            rollNo = sc.nextInt();

            System.out.print("Enter semester (1-6): ");
            semester = sc.nextInt();
            sc.nextLine();

            System.out.print("Enter username: ");
            username = sc.nextLine();

            if (!username.contains("$") && !username.contains("_") && !username.contains("@")) {
                throw new InvalidUsernameException();
            }

            password = PasswordMask.readPassword("Enter password: ");

            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO students_data(name, department, roll_no, semester, username, password) VALUES (?,?,?,?,?,?)"
            );
            ps.setString(1, name);
            ps.setString(2, department);
            ps.setInt(3, rollNo);
            ps.setInt(4, semester);
            ps.setString(5, username);
            ps.setString(6, password);
            ps.executeUpdate();

            System.out.println("Sign-up successful! You can now login.");
            login();

        } catch (InvalidUsernameException e) {
            System.out.println("Exception: " + e);
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Roll number or username already exists!");
        } catch (Exception e) {
            System.out.println("Error during sign up: " + e);
        }
    }

    // ---------- LOGIN ----------
    public void login() {
        Scanner sc = FacultyFeedbackSystemManager.sc;
        try {
            System.out.print("Enter username: ");
            username = sc.next();

            password = PasswordMask.readPassword("Enter password: ");

            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM students_data WHERE username=? AND password=?"
            );
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                rollNo = rs.getInt("roll_no");
                name = rs.getString("name");
                department = rs.getString("department");
                semester = rs.getInt("semester");

                System.out.println("\nLogin successful!");
                System.out.println("Welcome " + name + " (" + department + " - Sem " + semester + ")");

                boolean exit = false;
                do {
                    System.out.println("\n--- Student Menu ---");
                    System.out.println("1. Enter 1 to Change Password");
                    System.out.println("2. Enter 2 to Rate Teachers");
                    System.out.println("3. Enter 3 to Logout");
                    System.out.print("Enter choice: ");
                    int choice = sc.nextInt();
                    sc.nextLine(); // consume newline

                    switch (choice) {
                        case 1:
                            changePassword();
                            break;
                        case 2:
                            rateTeachersAutomatically();
                            break;
                        case 3:
                            exit = true;
                            System.out.println("Logging out...");
                            break;
                        default:
                            System.out.println("Invalid choice!");
                    }
                } while (!exit);

            } else {
                System.out.println("Invalid username or password.");
            }

        } catch (Exception e) {
            System.out.println("Login error: " + e);
        }
    }

    // ---------- CHANGE PASSWORD ----------
    void changePassword() {
        Scanner sc = FacultyFeedbackSystemManager.sc;
        try {
            String newPassword = PasswordMask.readPassword("Enter new password: ");
            PreparedStatement ps = con.prepareStatement(
                "UPDATE students_data SET password=? WHERE roll_no=?"
            );
            ps.setString(1, newPassword);
            ps.setInt(2, rollNo);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("Password updated successfully!");
                password = newPassword;
            } else {
                System.out.println("Failed to update password.");
            }
        } catch (Exception e) {
            System.out.println("Error updating password: " + e);
        }
    }

    // ---------- AUTOMATIC TEACHER RATING WITH SUBJECTS ----------
    void rateTeachersAutomatically() {
        Scanner sc = FacultyFeedbackSystemManager.sc;
        try {
            PreparedStatement psTeachers = con.prepareStatement(
                "SELECT t.teacher_id, t.name, GROUP_CONCAT(s.subject_name SEPARATOR ', ') AS subjects " +
                "FROM teachers_data t " +
                "LEFT JOIN subjects s ON t.teacher_id = s.teacher_id AND s.semester=? " +
                "WHERE t.department=? AND FIND_IN_SET(?, t.semester) " +
                "GROUP BY t.teacher_id, t.name"
            );
            psTeachers.setInt(1, semester);
            psTeachers.setString(2, department);
            psTeachers.setString(3, String.valueOf(semester));
            ResultSet rsTeachers = psTeachers.executeQuery();

            boolean hasTeachers = false;
            while (rsTeachers.next()) {
                hasTeachers = true;
                String teacherId = rsTeachers.getString("teacher_id");
                String teacherName = rsTeachers.getString("name");
                String subjects = rsTeachers.getString("subjects");

                System.out.println("\nRate Teacher: " + teacherName);
                System.out.println("Subjects: " + (subjects != null ? subjects : "No subjects assigned"));

                int rating = 0;
                while (true) {
                    System.out.print("Enter rating (1-5): ");
                    if (sc.hasNextInt()) {
                        rating = sc.nextInt();
                        sc.nextLine(); // consume newline
                        if (rating >= 1 && rating <= 5) break;
                        else System.out.println("Rating must be between 1 and 5.");
                    } else {
                        System.out.println("Please enter a number between 1 and 5.");
                        sc.next();
                    }
                }

                PreparedStatement psCheck = con.prepareStatement(
                    "SELECT * FROM ratings WHERE student_roll_no=? AND teacher_id=?"
                );
                psCheck.setInt(1, rollNo);
                psCheck.setString(2, teacherId);
                ResultSet rsCheck = psCheck.executeQuery();

                if (rsCheck.next()) {
                    PreparedStatement psUpdate = con.prepareStatement(
                        "UPDATE ratings SET rating=? WHERE student_roll_no=? AND teacher_id=?"
                    );
                    psUpdate.setInt(1, rating);
                    psUpdate.setInt(2, rollNo);
                    psUpdate.setString(3, teacherId);
                    psUpdate.executeUpdate();
                    System.out.println("Rating updated successfully!");
                } else {
                    PreparedStatement psInsert = con.prepareStatement(
                        "INSERT INTO ratings(student_roll_no, teacher_id, rating) VALUES (?,?,?)"
                    );
                    psInsert.setInt(1, rollNo);
                    psInsert.setString(2, teacherId);
                    psInsert.setInt(3, rating);
                    psInsert.executeUpdate();
                    System.out.println("Rating submitted successfully!");
                }
            }

            if (!hasTeachers) {
                System.out.println("No teachers found for your department and semester!");
            }

        } catch (Exception e) {
            System.out.println("Error rating teachers: " + e);
        }
    }
}

// ====================== HOD CLASS ======================
class HOD extends User {
    Connection con;
    String dept;

    HOD(String username, String password) {
        super(username, password);
        try {
            con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/feedback_system?useSSL=false&allowPublicKeyRetrieval=true",
                "root", "rishik@12345"
            );
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e);
        }
    }

    public void login() {
        Scanner sc = FacultyFeedbackSystemManager.sc;
        System.out.print("Enter username: ");
        username = sc.nextLine();
        password = PasswordMask.readPassword("Enter password: ");

        if (username.equals("HOD@SBMPIT") && password.equals("HOD$8794")) dept = "Information Technology";
        else if (username.equals("HOD@SBMPME") && password.equals("HOD$8795")) dept = "Mechanical";
        else if (username.equals("HOD@SBMPCSE") && password.equals("HOD$8796")) dept = "Computer Engineering";
        else if (username.equals("HOD@SBMPEC") && password.equals("HOD$8797")) dept = "Electronics";
        else {
            System.out.println("Invalid credentials!");
            return;
        }

        System.out.println("\nHOD Login Successful for " + dept + " Department");
        showRatingsPortal();
    }

    void showRatingsPortal() {
        Scanner sc = FacultyFeedbackSystemManager.sc;
        int semChoice;
        do {
            System.out.println("\n--- HOD PORTAL ---");
            System.out.println("Select semester to view ratings (1-6)");
            System.out.println("7. Logout");
            System.out.print("Enter choice: ");
            while (!sc.hasNextInt()) {
                System.out.println("Please enter a valid number!");
                sc.next();
            }
            semChoice = sc.nextInt();
            sc.nextLine(); // consume newline

            if (semChoice >= 1 && semChoice <= 6) {
                showRatingsChart(semChoice);      // Bar chart for teacher ratings
                showVotingPieChart(semChoice);    // Pie chart for student voting
            } else if (semChoice == 7) {
                System.out.println("Logging out...");
            } else {
                System.out.println("Invalid choice!");
            }
        } while (semChoice != 7);
    }

    // ---------- BAR CHART ----------
    void showRatingsChart(int semester) {
        try {
            String query =
                "SELECT t.name, COALESCE(AVG(r.rating),0) AS avg_rating " +
                "FROM teachers_data t LEFT JOIN ratings r ON t.teacher_id = r.teacher_id " +
                "LEFT JOIN students_data s ON r.student_roll_no = s.roll_no " +
                "WHERE t.department=? AND s.semester=? " +
                "GROUP BY t.name ORDER BY avg_rating DESC";

            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, dept);
            ps.setInt(2, semester);
            ResultSet rs = ps.executeQuery();

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                dataset.addValue(rs.getDouble("avg_rating"), "Rating", rs.getString("name"));
            }

            if (!hasData) {
                System.out.println("No ratings found for semester " + semester + " in " + dept + " department.");
                return;
            }

            JFreeChart chart = ChartFactory.createBarChart(
                "Faculty Feedback Ratings - " + dept + " (Semester " + semester + ")",
                "Teacher Name",
                "Average Rating (out of 5)",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
            );

            CategoryPlot plot = chart.getCategoryPlot();
            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, Color.BLUE);

            JFrame frame = new JFrame("Faculty Ratings Dashboard - " + dept + " (Sem " + semester + ")");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.add(new ChartPanel(chart));
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- PIE CHART ----------
    void showVotingPieChart(int semester) {
        try {
            PreparedStatement psTotal = con.prepareStatement(
                "SELECT COUNT(*) AS total FROM students_data WHERE department=? AND semester=?"
            );
            psTotal.setString(1, dept);
            psTotal.setInt(2, semester);
            ResultSet rsTotal = psTotal.executeQuery();
            int totalStudents = 0;
            if (rsTotal.next()) totalStudents = rsTotal.getInt("total");

            PreparedStatement psVoted = con.prepareStatement(
                "SELECT COUNT(DISTINCT student_roll_no) AS voted FROM ratings r " +
                "JOIN students_data s ON r.student_roll_no = s.roll_no " +
                "WHERE s.department=? AND s.semester=?"
            );
            psVoted.setString(1, dept);
            psVoted.setInt(2, semester);
            ResultSet rsVoted = psVoted.executeQuery();
            int votedStudents = 0;
            if (rsVoted.next()) votedStudents = rsVoted.getInt("voted");

            int notVotedStudents = totalStudents - votedStudents;

            DefaultPieDataset dataset = new DefaultPieDataset();
            dataset.setValue("Voted", votedStudents);
            dataset.setValue("Not Voted", notVotedStudents);

            JFreeChart pieChart = ChartFactory.createPieChart(
                "Voting Status - Semester " + semester + " (" + dept + ")",
                dataset,
                true, true, false
            );

            PiePlot plot = (PiePlot) pieChart.getPlot();
            plot.setSectionPaint("Voted", Color.GREEN);
            plot.setSectionPaint("Not Voted", Color.RED);

            JFrame frame = new JFrame("Voting Status Pie Chart - " + dept + " (Sem " + semester + ")");
            frame.setSize(600, 400);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ChartPanel(pieChart));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// ====================== MAIN CLASS ======================
public class FacultyFeedbackSystemManager {
    public static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        int ch;

        do {
            System.out.println("\n====== FACULTY FEEDBACK SYSTEM ======");
            System.out.println("1. Enter 1 to login Student Portal");
            System.out.println("2. Enter 2 to HOD Portal");
            System.out.println("3. Enter 3 to Exit");
            System.out.print("Enter your choice: ");

            while (!sc.hasNextInt()) {
                System.out.println("Please enter a valid number!");
                sc.next();
            }

            ch = sc.nextInt();
            sc.nextLine(); // clear newline

            switch (ch) {
                case 1:
                    Student s = new Student("", "");
                    System.out.println("\n1. Sign Up");
                    System.out.println("2. Login");
                    System.out.print("Enter choice: ");
                    int choice = sc.nextInt();
                    sc.nextLine();
                    if (choice == 1) s.signUp();
                    else if (choice == 2) s.login();
                    else System.out.println("Invalid choice!");
                    break;

                case 2:
                    HOD h = new HOD("", "");
                    h.login();
                    break;

                case 3:
                    System.out.println("Exiting system...");
                    break;

                default:
                    System.out.println("Invalid choice! Try again.");
            }
        } while (ch != 3);
    }
}
