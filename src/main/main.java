import config.config;
import static config.config.connectDB;
import java.sql.*;
import java.util.Scanner;

public class main {
    
    static config db = new config();
    static String role, status;
   
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        

        while (true) {
            System.out.println("\n==== Barangay Document Request System ====");
            System.out.println("1. Log In");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
            case 1:
                System.out.print("Enter Username: ");
                String username = sc.nextLine();
                System.out.print("Enter Password: ");
                String password = sc.nextLine();
                

                try {
                    

                    
                        PreparedStatement state = connectDB().prepareStatement("SELECT u_status, u_role, u_password FROM tbl_user WHERE u_name = '"+username+"' AND u_password = '"+password+"'");
                       
                        if(checkLogin(username, password)) {
                            try (ResultSet rs = state.executeQuery()) {
                                if(rs.next()) {
                                    role = rs.getString("u_role");
                                    status = rs.getString("u_status");   
                                }
                                rs.close();
                                
                                if (status.equalsIgnoreCase("Approved")) {
                                    System.out.println(" Login successful! Welcome, " + username);

                                    if (role.equalsIgnoreCase("Official")) {
                                        showAdminMenu(sc); // go to admin panel
                                    } else {
                                        System.out.println("You are logged in as a normal user.");
                                    }

                                } else if (status.equalsIgnoreCase("Pending")) {
                                    System.out.println("PENDING");
                                }
                                else {
                                    System.out.println("Your account is still pending approval.");
                                }
                            }
                            
                        }
                } catch (SQLException e) {
                    System.out.println("Error: " + e.getMessage());
                }
                break;

            case 2:
                System.out.print("Enter Username: ");
                String newUser = sc.nextLine();
                System.out.print("Enter Password: ");
                String newType = sc.nextLine();

                
                db.updateRecord("INSERT INTO tbl_user (u_name, u_password, u_status) VALUES (?, ?, 'Pending')",
                        newUser, newType);

                System.out.println("Registration successful! Please wait for barangay official's approval.");
                break;

            case 3: // EXIT
                System.out.println("Exiting system...");
                sc.close();
                return;

            default:
                System.out.println("Invalid option, try again.");
}

        }
    }

   
    private static boolean checkLogin(String username, String password) {
        String sql = "SELECT * FROM tbl_user WHERE u_name = ? AND u_password = ?";
        try (Connection conn = config.connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try(ResultSet rs = pstmt.executeQuery()) {
                if(rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error checking login: " + e.getMessage());
        }
        return false;
    }

    
    public static void showAdminMenu(Scanner sc) {
        while (true) {
            System.out.println("\n==== Admin Menu ====");
            System.out.println("1. Register");
            System.out.println("2. Update Information");
            System.out.println("3. View Data");
            System.out.println("4. Delete Information");
            System.out.println("5. Approve Registrations");
            System.out.println("6. Logout");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1: // ADD RECORD
                    System.out.print("Enter Username: ");
                    String uname = sc.nextLine();
                    System.out.print("Enter Password: ");
                    String utype = sc.nextLine();
                    System.out.println("Enter contact number: ");
                    String contact = sc.nextLine();

                    String addSql = "INSERT INTO tbl_user (u_name, u_password, u_contact) VALUES (?, ?, ?)";
                    db.addRecord(addSql, uname, utype, contact);
                    break;

                case 2: // UPDATE RECORD
                    System.out.print("Enter User ID to update: ");
                    int uid = sc.nextInt();
                    sc.nextLine();
                    System.out.print("Enter new Username: ");
                    String newName = sc.nextLine();
                    System.out.print("Enter new password: ");
                    String newType = sc.nextLine();
                    System.out.println("Enter contact number: ");
                    String newContact = sc.nextLine();

                    String updateSql = "UPDATE tbl_user SET u_name = ?, u_password = ?, u_contact = ? WHERE u_id = ?";
                    db.updateRecord(updateSql, newName, newType, uid);
                    break;

                case 3: // VIEW RECORDS
                    String[] headers = {"ID", "Name", "Role", "Contact", "Status" };
                    String[] cols = {"u_id", "u_name", "u_role", "u_contact", "u_status"};
                    db.viewRecords("SELECT * FROM tbl_user", headers, cols);
                    break;

                case 4: // DELETE RECORD
                    System.out.print("Enter User ID to delete: ");
                    int delId = sc.nextInt();
                    sc.nextLine();

                    String deleteSql = "DELETE FROM tbl_user WHERE u_id = ?";
                    db.deleteRecord(deleteSql, delId);
                    break;

                case 5:
                
                
                
                String sql = "SELECT u_id, u_name, u_password, u_status FROM tbl_user WHERE u_status = 'Pending'";
                String[] citizensHeaders = {"ID", "Username", "Password", "Status"};
                String[] citizensColumns = {"u_id", "u_name", "u_password", "u_status"};

                db.viewRecords(sql, citizensHeaders, citizensColumns);
                


                    System.out.print("Enter ID to approve/deny: ");
                    int id = sc.nextInt();
                    sc.nextLine(); // consume newline
                    System.out.print("Approve or Deny? (A/D): ");
                    String decision = sc.nextLine().trim();
                    
                    String update = "UPDATE tbl_user SET u_status = ? WHERE u_id = ?";
                    String status = null;

                    if (decision.equalsIgnoreCase("A")) {
                        status = "Approved";
                    } else if (decision.equalsIgnoreCase("D")) {
                        status = "Denied";
                    } else {
                        System.out.println("Invalid choice.");
                    }
                    
                    db.updateRecord(update, status, id);
                
                break;    
                    
                case 6: // LOGOUT
                    System.out.println("?Logging out...");
                    return;

                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }
}