import config.config;
import static config.config.connectDB;
import java.sql.*;
import java.util.Scanner;

public class main {

    static config db = new config();
    static String role, status, dbPassword, fullName;
    static int userId;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n==== Barangay Document Request System ====");
            System.out.println("1. Log In");
            System.out.println("2. Online Registration");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    boolean loggedIn = false;

                    while (!loggedIn) {
                        System.out.print("Enter Username (u_name): ");
                        String username = sc.nextLine();
                        System.out.print("Enter Password: ");
                        String password = sc.nextLine();

                        try (Connection conn = connectDB()) {
                            PreparedStatement state = conn.prepareStatement(
                                "SELECT u_id, u_status, u_role, u_password, u_fullname FROM tbl_user WHERE u_name = ?"
                            );
                            state.setString(1, username);

                            try (ResultSet rs = state.executeQuery()) {
                                if (rs.next()) {
                                    dbPassword = rs.getString("u_password");
                                    role = rs.getString("u_role");
                                    status = rs.getString("u_status");
                                    fullName = rs.getString("u_fullname");
                                    userId = rs.getInt("u_id");
                                }
                                // ResultSet closer to prevent database locking
                                rs.close();
                                
                                
                                // DO NOT PUT THIS IF STATEMENT INSIDE THE if(rs.next()) CONDITIONAL STATEMENT BECAUSE IT'LL LOCK THE DATABASE
                                if (password.equals(dbPassword)) {
                                        if (status.equalsIgnoreCase("Approved")) {
                                            System.out.println("Login successful! Welcome, " + fullName);

                                            if (role.equalsIgnoreCase("Official") || role.equalsIgnoreCase("Admin")) {
                                                showAdminMenu(sc);
                                            } else {
                                                showResidentMenu(sc, userId);
                                            }
                                            loggedIn = true;
                                        } else if (status.equalsIgnoreCase("Pending")) {
                                            System.out.println("Your account is still pending approval.");
                                            loggedIn = true;
                                        } else {
                                            System.out.println("Your account is denied or inactive.");
                                            loggedIn = true;
                                        }
                                    } else {
                                        System.out.println("Incorrect password! Please try again...");
                                    }
                            }
                        } catch (SQLException e) {
                            System.out.println("Error: " + e.getMessage());
                        }

                        if (!loggedIn) {
                            System.out.println(); // spacing for readability
                        }
                    }
                    break;

                case 2: // USER REGISTER
                    String newUser;
                    String newType;
                    String newContact;
                    String newFullName;

                    while (true) {
                        System.out.print("Enter Username (u_name): ");
                        newUser = sc.nextLine();

                        try (Connection conn = connectDB();
                             PreparedStatement checkStmt = conn.prepareStatement(
                                 "SELECT COUNT(*) FROM tbl_user WHERE u_name = ?"
                             )) {

                            // ✅ Set the parameter BEFORE executing the query
                            checkStmt.setString(1, newUser);

                            try (ResultSet rs = checkStmt.executeQuery()) {
                                rs.next();
                                if (rs.getInt(1) > 0) {
                                    System.out.println("❌ Username already exists! Please try another one.\n");
                                    continue; // go back to top of loop
                                }
                            }

                            // ✅ Username is unique, break out of the loop
                            break;

                        } catch (SQLException e) {
                            System.out.println("❌ Error checking username: " + e.getMessage());
                            return;
                        }
                    }

                    System.out.print("Enter Full Name: ");
                    newFullName = sc.nextLine();
                    System.out.print("Enter Password: ");
                    newType = sc.nextLine();

                    // ✅ Contact number validation (11 digits only)
                    while (true) {
                        System.out.print("Enter Contact Number: ");
                        newContact = sc.nextLine();

                        if (!newContact.matches("\\d{11}")) {
                            System.out.println("❌ Invalid contact number! It must be exactly 11 digits.\n");
                            continue;
                        }
                        break;
                    }

                    // ✅ Insert new record
                    db.updateRecord(
                        "INSERT INTO tbl_user (u_name, u_fullname, u_password, u_status, u_contact) VALUES (?, ?, ?, 'Pending', ?)",
                        newUser, newFullName, newType, newContact
                    );
                    System.out.println("✅ Registration successful! Please wait for approval.");
                    break;


                case 3:
                    System.out.println("Exiting system...");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid option, try again.");
            }
        }
    }

                public static void showAdminMenu(Scanner sc) {
                while (true) {
                    System.out.println("\n==== Admin Menu ====");
                    System.out.println("1. Register");
                    System.out.println("2. Update Information");
                    System.out.println("3. View Data");
                    System.out.println("4. Delete Information");
                    System.out.println("5. Approve Registrations");
                    System.out.println("6. Approve Document Requests");
                    System.out.println("7. Logout");
                    System.out.print("Choose an option: ");
                    int choice = sc.nextInt();
                    sc.nextLine();

                    switch (choice) {
                        case 1:
                            String uname, ufullname, utype, contact;
                            try (Connection conn = connectDB()) {
                                // 🔹 Validate Username (must be unique)
                                while (true) {
                                    System.out.print("Enter Username (Login name): ");
                                    uname = sc.nextLine();
                                    PreparedStatement checkUser = conn.prepareStatement(
                                            "SELECT COUNT(*) FROM tbl_user WHERE u_name = ?");
                                    checkUser.setString(1, uname);
                                    ResultSet rsUser = checkUser.executeQuery();
                                    rsUser.next();
                                    if (rsUser.getInt(1) > 0) {
                                        System.out.println("❌ Username already exists! Please try another.\n");
                                        rsUser.close();
                                        checkUser.close();
                                        continue; // ask again
                                    }
                                    rsUser.close();
                                    checkUser.close();
                                    break; // valid username
                                }

                                while (true) {
                                    System.out.print("Enter Full Name: ");
                                    ufullname = sc.nextLine();
                                    PreparedStatement checkFull = conn.prepareStatement(
                                            "SELECT COUNT(*) FROM tbl_user WHERE u_fullname = ?");
                                    checkFull.setString(1, ufullname);
                                    ResultSet rsFull = checkFull.executeQuery();
                                    rsFull.next();
                                    if (rsFull.getInt(1) > 0) {
                                        System.out.println("❌ Full Name already exists! Please try another.\n");
                                        rsFull.close();
                                        checkFull.close();
                                        continue;
                                    }
                                    rsFull.close();
                                    checkFull.close();
                                    break;
                                }

                                System.out.print("Enter Password: ");
                                utype = sc.nextLine();

                                while (true) {
                                    System.out.print("Enter Contact Number: ");
                                    contact = sc.nextLine();

                                    // ✅ Validation: only allow 11 digits
                                    if (!contact.matches("\\d{11}")) {
                                        System.out.println("❌ Invalid contact number! It must contain exactly 11 digits.\n");
                                        continue; // ask again
                                    }

                                    // ✅ Check if contact number already exists in the database
                                    PreparedStatement checkContact = conn.prepareStatement(
                                        "SELECT COUNT(*) FROM tbl_user WHERE u_contact = ?"
                                    );
                                    checkContact.setString(1, contact);
                                    ResultSet rsContact = checkContact.executeQuery();
                                    rsContact.next();

                                    if (rsContact.getInt(1) > 0) {
                                        System.out.println("❌ Contact number already exists! Please try another.\n");
                                        rsContact.close();
                                        checkContact.close();
                                        continue; // ask again
                                    }

                                    rsContact.close();
                                    checkContact.close();
                                    break; // exit loop if valid and unique
                                }


                                String addSql = "INSERT INTO tbl_user (u_name, u_fullname, u_password, u_contact) VALUES (?, ?, ?, ?)";
                                db.addRecord(addSql, uname, ufullname, utype, contact);
                                System.out.println("✅ User registered successfully!");
                            } catch (SQLException e) {
                                System.out.println("Error checking duplicates: " + e.getMessage());
                            }
                            break;

                        case 2:
                            // 🔹 Show existing records before asking for ID
                        String sql = "SELECT u_id, u_name, u_fullname, u_password, u_contact, u_status FROM tbl_user";
                        String[] userHeaders = {"ID", "Username", "Full Name", "Password", "Contact", "Status"};
                        String[] userColumns = {"u_id", "u_name", "u_fullname", "u_password", "u_contact", "u_status"};
                        db.viewRecords(sql, userHeaders, userColumns);

                        // 🔹 Ask for ID and validate it exists
                        int uid;
                        while (true) {
                            System.out.print("Enter User ID to update: ");
                            uid = sc.nextInt();
                            sc.nextLine();

                            try (Connection conn = connectDB()) {
                                PreparedStatement checkStmt = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM tbl_user WHERE u_id = ?"
                                );
                                checkStmt.setInt(1, uid);
                                ResultSet rs = checkStmt.executeQuery();
                                rs.next();

                                if (rs.getInt(1) == 0) {
                                    System.out.println("❌ ID not found! Please enter a valid ID.\n");
                                    rs.close();
                                    checkStmt.close();
                                    continue;
                                }

                                rs.close();
                                checkStmt.close();
                                break; // valid ID found, continue to update section

                            } catch (SQLException e) {
                                System.out.println("Error checking ID: " + e.getMessage());
                                return;
                            }
                        }

                        String newName;
                        String newFullName;
                        String newType;
                        String newContact;

                        // 🔹 Check for duplicate Username
                        while (true) {
                            System.out.print("Enter new Username (Login name): ");
                            newName = sc.nextLine();
                            try (Connection conn = connectDB()) {
                                PreparedStatement checkStmt = conn.prepareStatement(
                                        "SELECT COUNT(*) FROM tbl_user WHERE u_name = ? AND u_id != ?");
                                checkStmt.setString(1, newName);
                                checkStmt.setInt(2, uid);
                                ResultSet rs = checkStmt.executeQuery();
                                rs.next();
                                if (rs.getInt(1) > 0) {
                                    System.out.println("Username already exists! Please try another one.\n");
                                    rs.close();
                                    checkStmt.close();
                                    continue;
                                }
                                rs.close();
                                checkStmt.close();
                                break;
                            } catch (SQLException e) {
                                System.out.println("Error checking username: " + e.getMessage());
                                return;
                            }
                        }

                        // 🔹 Check for duplicate Full Name
                        while (true) {
                            System.out.print("Enter new Full Name: ");
                            newFullName = sc.nextLine();
                            try (Connection conn = connectDB()) {
                                PreparedStatement checkStmt = conn.prepareStatement(
                                        "SELECT COUNT(*) FROM tbl_user WHERE u_fullname = ? AND u_id != ?");
                                checkStmt.setString(1, newFullName);
                                checkStmt.setInt(2, uid);
                                ResultSet rs = checkStmt.executeQuery();
                                rs.next();
                                if (rs.getInt(1) > 0) {
                                    System.out.println("Full Name already exists! Please try another one.\n");
                                    rs.close();
                                    checkStmt.close();
                                    continue;
                                }
                                rs.close();
                                checkStmt.close();
                                break;
                            } catch (SQLException e) {
                                System.out.println("Error checking full name: " + e.getMessage());
                                return;
                            }
                        }

                        System.out.print("Enter new password: ");
                        newType = sc.nextLine();

                        // 🔹 Check for duplicate Contact Number
                        while (true) {
                            System.out.print("Enter new contact number: ");
                            newContact = sc.nextLine();
                            try (Connection conn = connectDB()) {
                                PreparedStatement checkStmt = conn.prepareStatement(
                                        "SELECT COUNT(*) FROM tbl_user WHERE u_contact = ? AND u_id != ?");
                                checkStmt.setString(1, newContact);
                                checkStmt.setInt(2, uid);
                                ResultSet rs = checkStmt.executeQuery();
                                rs.next();
                                if (rs.getInt(1) > 0) {
                                    System.out.println("Contact number already exists! Please try another one.\n");
                                    rs.close();
                                    checkStmt.close();
                                    continue;
                                }
                                rs.close();
                                checkStmt.close();
                                break;
                            } catch (SQLException e) {
                                System.out.println("Error checking contact number: " + e.getMessage());
                                return;
                            }
                        }

                        // 🔹 Proceed with update if all checks pass
                        String updateSql = "UPDATE tbl_user SET u_name = ?, u_fullname = ?, u_password = ?, u_contact = ? WHERE u_id = ?";
                        db.updateRecord(updateSql, newName, newFullName, newType, newContact, uid);
                        System.out.println("✅ User information updated successfully!");
                        break;


                        case 3:
                            String[] headers = {"ID", "Username", "Full Name", "Role", "Contact", "Status"};
                            String[] cols = {"u_id", "u_name", "u_fullname", "u_role", "u_contact", "u_status"};
                            db.viewRecords("SELECT * FROM tbl_user", headers, cols);
                            break;

                        case 4: {
                            // Show existing records before deleting
                            String deletesql = "SELECT u_id, u_name, u_fullname, u_contact, u_status FROM tbl_user";
                            String[] header = {"ID", "Username", "Full Name", "Contact", "Status"};
                            String[] columns = {"u_id", "u_name", "u_fullname", "u_contact", "u_status"};
                            db.viewRecords(deletesql, header, columns);

                            int delId;

                            // Validation loop: keeps asking until valid ID is entered
                            while (true) {
                                System.out.print("Enter User ID to delete: ");
                                delId = sc.nextInt();
                                sc.nextLine();

                                try {
                                    PreparedStatement checkStmt = connectDB().prepareStatement(
                                        "SELECT COUNT(*) FROM tbl_user WHERE u_id = ?"
                                    );
                                    checkStmt.setInt(1, delId);
                                    ResultSet rs = checkStmt.executeQuery();
                                    rs.next();

                                    if (rs.getInt(1) == 0) {
                                        System.out.println("❌ ID not found! Please enter a valid ID.\n");
                                        rs.close();
                                        checkStmt.close();
                                        continue; // ask again
                                    }

                                    rs.close();
                                    checkStmt.close();
                                    break; // valid ID found, exit loop
                                } catch (SQLException e) {
                                    System.out.println("Error checking ID: " + e.getMessage());
                                    return;
                                }
                            }

                            // Proceed to delete
                            String deleteSql = "DELETE FROM tbl_user WHERE u_id = ?";
                            db.deleteRecord(deleteSql, delId);
                            System.out.println("✅ User deleted successfully!");
                            break;
}



                        case 5:
                            String pendingSql = "SELECT u_id, u_name, u_password, u_status FROM tbl_user WHERE u_status = 'Pending'";

                            String[] citizensHeaders = {"ID", "Username", "Password", "Status"};
                            String[] citizensColumns = {"u_id", "u_name", "u_password", "u_status"};
                            db.viewRecords(pendingSql, citizensHeaders, citizensColumns);

                            int id;
                            while (true) {
                                System.out.print("Enter ID to approve/deny: ");
                                id = sc.nextInt();
                                sc.nextLine();

                                try {
                                    PreparedStatement checkStmt = connectDB().prepareStatement(
                                        "SELECT COUNT(*) FROM tbl_user WHERE u_id = ? AND u_status = 'Pending'"
                                    );
                                    checkStmt.setInt(1, id);
                                    ResultSet rs = checkStmt.executeQuery();
                                    rs.next();

                                    if (rs.getInt(1) == 0) {
                                        System.out.println("❌ ID not found in pending list! Please enter a valid ID.\n");
                                        rs.close();
                                        checkStmt.close();
                                        continue; // loop back to ask again
                                    }

                                    rs.close();
                                    checkStmt.close();
                                    break; // valid ID found, exit loop

                                } catch (SQLException e) {
                                    System.out.println("Error checking ID: " + e.getMessage());
                                    return;
                                }
                            }

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

                        case 6: {
                            System.out.println("\n==== Pending Document Requests ====");
                            String[] rHeaders = {"Request ID", "User ID", "Document Type", "Fee", "Purpose", "Date", "Status"};
                            String[] rCols = {"r_id", "u_id", "d_doctype", "d_fee", "r_purpose", "r_date", "r_status"};

                            // Join tbl_req and tbl_doc using the document name (d_doctype)
                            String viewQuery =
                                "SELECT r.r_id, r.u_id, d.d_doctype, d.d_fee, r.r_purpose, r.r_date, r.r_status " +
                                "FROM tbl_req AS r " +
                                "JOIN tbl_doc AS d ON r.r_purpose = d.d_doctype " +
                                "WHERE r.r_status = 'Pending'";

                            db.viewRecords(viewQuery, rHeaders, rCols);

                            int rid;

                            // ✅ Validation loop for Request ID
                            while (true) {
                                System.out.print("Enter Request ID to process: ");
                                rid = sc.nextInt();
                                sc.nextLine();

                                try {
                                    PreparedStatement checkStmt = connectDB().prepareStatement(
                                        "SELECT COUNT(*) FROM tbl_req WHERE r_id = ? AND r_status = 'Pending'"
                                    );
                                    checkStmt.setInt(1, rid);
                                    ResultSet rs = checkStmt.executeQuery();
                                    rs.next();

                                    if (rs.getInt(1) == 0) {
                                        System.out.println("❌ Request ID not found or not pending! Please enter a valid ID.\n");
                                        rs.close();
                                        checkStmt.close();
                                        continue; // ask again
                                    }

                                    rs.close();
                                    checkStmt.close();
                                    break; // valid ID found, proceed
                                } catch (SQLException e) {
                                    System.out.println("Error checking Request ID: " + e.getMessage());
                                    return;
                                }
                            }

                            System.out.print("Approve or Deny? (A/D): ");
                            String decisionDoc = sc.nextLine().trim();

                            String statusDoc = null;
                            if (decisionDoc.equalsIgnoreCase("A")) {
                                statusDoc = "Approved";
                            } else if (decisionDoc.equalsIgnoreCase("D")) {
                                statusDoc = "Denied";
                            } else {
                                System.out.println("Invalid choice.");
                                break;
                            }

                            String updateDoc = "UPDATE tbl_req SET r_status = ? WHERE r_id = ?";
                            db.updateRecord(updateDoc, statusDoc, rid);

                            System.out.println("✅ Document request " + statusDoc.toLowerCase() + " successfully!");
                            break;
                        }




                        case 7:
                            System.out.println("Logging out...");
                            return;

                        default:
                            System.out.println("Invalid option. Try again.");
                    }
                }
            }




    // 🔹 RESIDENT PANEL
    public static void showResidentMenu(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n==== Resident Panel ====");
            System.out.println("1. Request Document");
            System.out.println("2. View My Profile");
            System.out.println("3. Logout");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    requestDocument(sc, userId);
                    break;
                case 2:
                    viewProfile(userId);
                    break;
                case 3:
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    public static void requestDocument(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n==== Request Document ====");
            System.out.println("1. Barangay Certificate of Residency (₱25)");
            System.out.println("2. Barangay Clearance (₱15)");
            System.out.println("3. Barangay Business Permit (₱30)");
            System.out.println("4. Go Back");
            System.out.print("Choose document type: ");
            int choice = sc.nextInt();
            sc.nextLine();

            String docType = "";
            double fee = 0;

            switch (choice) {
                case 1:
                    docType = "Barangay Certificate of Residency";
                    fee = 25;
                    break;
                case 2:
                    docType = "Barangay Clearance";
                    fee = 15;
                    break;
                case 3:
                    docType = "Barangay Business Permit";
                    fee = 30;
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid option.");
                    continue;
            }

            System.out.print("Enter Purpose: ");
            String purpose = sc.nextLine();

            String insertSql = "INSERT INTO tbl_req (u_id, d_doctype, r_fee, r_purpose) VALUES (?, ?, ?, ?)";
            db.updateRecord(insertSql, userId, docType, fee, purpose);
            System.out.println("Request submitted successfully! Status: Pending");
            return;
        }
    }

    public static void viewProfile(int userId) {
        String sql = "SELECT u_id, u_name, u_fullname, u_role, u_contact, u_status FROM tbl_user WHERE u_id = " + userId;
        String[] headers = {"ID", "Username", "Full Name", "Role", "Contact", "Status"};
        String[] cols = {"u_id", "u_name", "u_fullname", "u_role", "u_contact", "u_status"};
        db.viewRecords(sql, headers, cols);
    }
}