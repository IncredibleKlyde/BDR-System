
import config.config;
import static config.config.connectDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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
                                            //officials Officials =new Officials();
                                            //officials.Officials();
                                            showAdminMenu(sc);
                                        } else {
                                            showResidentMenu(sc, userId);
                                        }
                                        loggedIn = true;
                                    } else if (status.equalsIgnoreCase("Pending")) {
                                        System.out.println("Your account is still pending approval.");
                                        loggedIn = true;
                                    } else {
                                        System.out.println("Your account has been disabled.");
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

                case 2: // USER REGISTERRF
                    String newUser;
                    String newPass;
                    String newContact;
                    String newFullName;
                    String newRole;

                    // 🔹 Check username uniqueness
                    while (true) {
                        System.out.print("Enter Username (u_name): ");
                        newUser = sc.nextLine();

                        try (Connection conn = connectDB()) {
                            PreparedStatement checkStmt = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM tbl_user WHERE u_name = ?"
                            );
                            checkStmt.setString(1, newUser);
                            ResultSet rs = checkStmt.executeQuery();

                            boolean exists = false;
                            if (rs.next() && rs.getInt(1) > 0) {
                                exists = true;
                            }

                            rs.close();        // ✅ close ResultSet immediately
                            checkStmt.close(); // ✅ close PreparedStatement immediately

                            if (exists) {
                                System.out.println("❌ Username already exists! Please try another one.\n");
                                continue; // ask again
                            }
                            break; // valid username

                        } catch (SQLException e) {
                            System.out.println("❌ Error checking username: " + e.getMessage());
                            return;
                        }
                    }

                    // 🔹 Enter Full Name
                    System.out.print("Enter Full Name: ");
                    newFullName = sc.nextLine();

                    // 🔹 Enter Password
                    System.out.print("Enter Password: ");
                    newPass = sc.nextLine();

                    //
                    String hashedPassword = db.hashPassword(newPass);

                    // 🔹 Validate Contact Number
                    while (true) {
                        System.out.print("Enter Contact Number: ");
                        newContact = sc.nextLine();

                        if (!newContact.matches("\\d{11}")) {
                            System.out.println("❌ Invalid contact number! It must be exactly 11 digits.\n");
                            continue;
                        }

                        try (Connection conn = connectDB()) {
                            PreparedStatement checkContact = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM tbl_user WHERE u_contact = ?"
                            );
                            checkContact.setString(1, newContact);
                            ResultSet rsContact = checkContact.executeQuery();

                            boolean contactExists = false;
                            if (rsContact.next() && rsContact.getInt(1) > 0) {
                                contactExists = true;
                            }

                            rsContact.close();        // ✅ prevent DB lock
                            checkContact.close();     // ✅ safely closed

                            if (contactExists) {
                                System.out.println("❌ Contact number already exists! Please try another.\n");
                                continue;
                            }
                        } catch (SQLException e) {
                            System.out.println("❌ Error checking contact: " + e.getMessage());
                            continue;
                        }

                        break; // valid contact
                    }

                    // 🔹 Ask for role (Official or Resident)
                    while (true) {
                        System.out.print("What user type are you? (Official or Resident): ");
                        newRole = sc.nextLine().trim();

                        if (newRole.equalsIgnoreCase("Official") || newRole.equalsIgnoreCase("Resident")) {
                            // capitalize properly (Official, Resident)
                            newRole = Character.toUpperCase(newRole.charAt(0)) + newRole.substring(1).toLowerCase();
                            break;
                        } else {
                            System.out.println("❌ Only choose 'Official' or 'Resident'.\n");
                        }
                    }

                    // 🔹 Insert new record safely
                    try {
                        db.updateRecord(
                                "INSERT INTO tbl_user (u_name, u_fullname, u_password, u_status, u_contact, u_role) VALUES (?, ?, ?, 'Pending', ?, ?)",
                                newUser, newFullName, hashedPassword, newContact, newRole
                        );

                        System.out.println("✅ Registration successful! Please wait for approval.");
                    } catch (Exception e) {
                        System.out.println("❌ Error during registration: " + e.getMessage());
                    }

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
            System.out.println("2. Update Account");
            System.out.println("3. View Data");
            System.out.println("4. Delete Account");
            System.out.println("5. Approve Registrations for Account");
            System.out.println("6. Approve/Update Document Requests");
            System.out.println("7. Disable Account");
            System.out.println("8. Document Panel");
            System.out.println("9. View Request Logs");
            System.out.println("10. Log out");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    String uname,
                     ufullname,
                     upass,
                     contact,
                     utype;

                    try (Connection conn = connectDB()) {

                        // 🔹 Validate Username (must be unique)
                        while (true) {
                            System.out.print("Enter Username (Login name): ");
                            uname = sc.nextLine();

                            PreparedStatement checkUser = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM tbl_user WHERE u_name = ?");
                            checkUser.setString(1, uname);
                            ResultSet rsUser = checkUser.executeQuery();

                            boolean exists = false;
                            if (rsUser.next() && rsUser.getInt(1) > 0) {
                                exists = true;
                            }

                            rsUser.close();
                            checkUser.close(); // ✅ close before running anything else

                            if (exists) {
                                System.out.println("❌ Username already exists! Please try another.\n");
                                continue; // ask again
                            }
                            break; // valid username
                        }

                        // 🔹 Validate Full Name (must be unique)
                        while (true) {
                            System.out.print("Enter Full Name: ");
                            ufullname = sc.nextLine();

                            PreparedStatement checkFull = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM tbl_user WHERE u_fullname = ?");
                            checkFull.setString(1, ufullname);
                            ResultSet rsFull = checkFull.executeQuery();

                            boolean fullExists = false;
                            if (rsFull.next() && rsFull.getInt(1) > 0) {
                                fullExists = true;
                            }

                            rsFull.close();
                            checkFull.close(); // ✅ safely closed

                            if (fullExists) {
                                System.out.println("❌ Full Name already exists! Please try another.\n");
                                continue;
                            }
                            break;
                        }

                        // 🔹 Enter Password
                        System.out.print("Enter Password: ");
                        upass = sc.nextLine();

                        // 🔹 Validate Contact Number (unique + 11 digits)
                        while (true) {
                            System.out.print("Enter Contact Number: ");
                            contact = sc.nextLine();

                            if (!contact.matches("\\d{11}")) {
                                System.out.println("❌ Invalid contact number! It must contain exactly 11 digits.\n");
                                continue;
                            }

                            PreparedStatement checkContact = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM tbl_user WHERE u_contact = ?");
                            checkContact.setString(1, contact);
                            ResultSet rsContact = checkContact.executeQuery();

                            boolean contactExists = false;
                            if (rsContact.next() && rsContact.getInt(1) > 0) {
                                contactExists = true;
                            }

                            rsContact.close();
                            checkContact.close(); // ✅ prevent lock

                            if (contactExists) {
                                System.out.println("❌ Contact number already exists! Please try another.\n");
                                continue;
                            }
                            break;
                        }

                        // 🔹 Ask for User Type (Official or Resident only)
                        while (true) {
                            System.out.print("What user type are you? (Official or Resident): ");
                            utype = sc.nextLine().trim();

                            if (utype.equalsIgnoreCase("Official") || utype.equalsIgnoreCase("Resident")) {
                                break; // valid input
                            } else {
                                System.out.println("❌ Only choose 'Official' or 'Resident'.\n");
                            }
                        }

                        // 🔹 Insert record safely
                        String addSql = "INSERT INTO tbl_user (u_name, u_fullname, u_password, u_contact, u_role) VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement addStmt = conn.prepareStatement(addSql);
                        addStmt.setString(1, uname);
                        addStmt.setString(2, ufullname);
                        addStmt.setString(3, upass);
                        addStmt.setString(4, contact);
                        addStmt.setString(5, utype);
                        addStmt.executeUpdate();
                        addStmt.close(); // ✅ final cleanup

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
                    String[] rHeaders = {"Request ID", "User ID", "Document Type", "Fee", "Purpose", "Date", "Status", "Approval By"};
                    String[] rCols = {"r_id", "u_id", "d_doctype", "d_fee", "r_purpose", "r_date", "r_status", "r_approvalby"};

                    // ✅ FIXED: Join by d_id instead of r_purpose
                    String viewQuery
                            = "SELECT r.r_id, r.u_id, d.d_doctype, d.d_fee, r.r_purpose, r.r_date, r.r_status, "
                            + "COALESCE(r.r_approvalby, 'Pending') AS r_approvalby "
                            + "FROM tbl_req AS r "
                            + "JOIN tbl_doc AS d ON r.d_id = d.d_id "
                            + // <-- fixed join
                            "WHERE r.r_status = 'Pending'";

                    db.viewRecords(viewQuery, rHeaders, rCols);

                    int rid;

                    while (true) {
                        System.out.print("Enter Request ID to process: ");
                        rid = sc.nextInt();
                        sc.nextLine();

                        try (Connection conn = connectDB()) {
                            PreparedStatement checkStmt = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM tbl_req WHERE r_id = ? AND r_status = 'Pending'"
                            );
                            checkStmt.setInt(1, rid);
                            ResultSet rs = checkStmt.executeQuery();
                            rs.next();

                            if (rs.getInt(1) == 0) {
                                System.out.println("❌ Request ID not found or not pending! Please enter a valid ID.\n");
                                continue;
                            }
                            break;
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

                    System.out.print("Enter your name (official approving/denying): ");
                    String approvalBy = sc.nextLine();

                    String updateDoc = "UPDATE tbl_req SET r_status = ?, r_approvalby = ? WHERE r_id = ?";
                    db.updateRecord(updateDoc, statusDoc, approvalBy, rid);

                    System.out.println("✅ Document request " + statusDoc.toLowerCase() + " successfully!");
                    break;
                }

                case 7: {
                    System.out.println("\n==== Disable Account ====");

                    // Show all active or approved users
                    String disableSql = "SELECT u_id, u_name, u_fullname, u_contact, u_status FROM tbl_user WHERE u_status = 'Approved'";
                    String[] disableHeaders = {"ID", "Username", "Full Name", "Contact", "Status"};
                    String[] disableCols = {"u_id", "u_name", "u_fullname", "u_contact", "u_status"};
                    db.viewRecords(disableSql, disableHeaders, disableCols);

                    int disableId;

                    // Validation loop to ensure user exists and is not already disabled
                    while (true) {
                        System.out.print("Enter User ID to disable: ");
                        disableId = sc.nextInt();
                        sc.nextLine();

                        try (Connection conn = connectDB()) {
                            PreparedStatement checkStmt = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM tbl_user WHERE u_id = ? AND u_status = 'Approved'"
                            );
                            checkStmt.setInt(1, disableId);
                            ResultSet rs = checkStmt.executeQuery();
                            rs.next();

                            if (rs.getInt(1) == 0) {
                                System.out.println("❌ ID not found or already disabled! Please enter a valid ID.\n");
                                rs.close();
                                checkStmt.close();
                                continue;
                            }

                            rs.close();
                            checkStmt.close();
                            break; // valid ID found

                        } catch (SQLException e) {
                            System.out.println("Error checking user: " + e.getMessage());
                            return;
                        }
                    }

                    // Confirm disable action
                    System.out.print("Are you sure you want to disable this account? (Y/N): ");
                    String confirm = sc.nextLine().trim();

                    if (confirm.equalsIgnoreCase("Y")) {
                        String disableUpdate = "UPDATE tbl_user SET u_status = 'Disabled' WHERE u_id = ?";
                        db.updateRecord(disableUpdate, disableId);
                        System.out.println("✅ Account disabled successfully!");
                    } else {
                        System.out.println("❌ Disable action cancelled.");
                    }

                    break;
                }

                case 8: // Document Panel
                    int docChoice;
                    do {
                        System.out.println("\n==== Document Panel ====");
                        System.out.println("1. Add document");
                        System.out.println("2. Update document");
                        System.out.println("3. View documents available to request");
                        System.out.println("4. Disable/Enable document");
                        System.out.println("5. Go back to Admin Panel");
                        System.out.print("Enter choice: ");
                        docChoice = sc.nextInt();
                        sc.nextLine(); // consume newline

                        switch (docChoice) {
                            case 1: // Add Document
                                System.out.print("Write the document name/type: ");
                                String docType = sc.nextLine();

                                System.out.print("Fee for that type of document: ");
                                int fee = sc.nextInt();
                                sc.nextLine(); // consume newline

                                try (Connection conn = connectDB();
                                        PreparedStatement pstmt = conn.prepareStatement(
                                                "INSERT INTO tbl_doc (d_doctype, d_fee, d_status) VALUES (?, ?, 'Available')")) {

                                    pstmt.setString(1, docType);
                                    pstmt.setInt(2, fee);
                                    pstmt.executeUpdate();
                                    System.out.println("✅ Document added successfully!");
                                } catch (SQLException e) {
                                    System.out.println("❌ Error adding document: " + e.getMessage());
                                }
                                break;

                            case 2: // Update Document                                        
                                try (Connection conn = connectDB()) {
                                    // 1️⃣ Display all documents first
                                    System.out.println("\n==== List of Documents ====");
                                    PreparedStatement viewStmt = conn.prepareStatement("SELECT d_id, d_doctype, d_fee, d_status FROM tbl_doc");
                                    ResultSet rsView = viewStmt.executeQuery();

                                    boolean hasDocs = false;
                                    while (rsView.next()) {
                                        hasDocs = true;
                                        System.out.printf("[%d] %-25s ₱%-5d  (%s)\n",
                                                rsView.getInt("d_id"),
                                                rsView.getString("d_doctype"),
                                                rsView.getInt("d_fee"),
                                                rsView.getString("d_status"));
                                    }
                                    rsView.close();
                                    viewStmt.close();

                                    if (!hasDocs) {
                                        System.out.println("⚠️  No documents found to update.");
                                        break;
                                    }

                                    // 2️⃣ Ask which document to update
                                    System.out.print("\nEnter the document ID to update: ");
                                    int updateId = sc.nextInt();
                                    sc.nextLine();

                                    // 3️⃣ Check if document exists
                                    PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM tbl_doc WHERE d_id = ?");
                                    checkStmt.setInt(1, updateId);
                                    ResultSet rs = checkStmt.executeQuery();

                                    if (!rs.next()) {
                                        System.out.println("❌ No document found with that ID.");
                                        break;
                                    }

                                    // 4️⃣ Ask for new details
                                    System.out.print("Enter new document name/type: ");
                                    String newwType = sc.nextLine();

                                    System.out.print("Enter new fee for that document: ");
                                    int newFee = sc.nextInt();
                                    sc.nextLine();

                                    // 5️⃣ Update document details
                                    PreparedStatement updateStmt = conn.prepareStatement(
                                            "UPDATE tbl_doc SET d_doctype = ?, d_fee = ? WHERE d_id = ?");
                                    updateStmt.setString(1, newwType);
                                    updateStmt.setInt(2, newFee);
                                    updateStmt.setInt(3, updateId);
                                    updateStmt.executeUpdate();

                                    System.out.println("✅ Document updated successfully!");

                                } catch (SQLException e) {
                                    System.out.println("❌ Error updating document: " + e.getMessage());
                                }
                                break;

                            case 3: // View Documents
                                try (Connection conn = connectDB();
                                        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tbl_doc");
                                        ResultSet rs = stmt.executeQuery()) {

                                    System.out.println("\n📜 Available Documents:");
                                    System.out.println("ID | Type | Fee | Status");
                                    System.out.println("----------------------------------");

                                    while (rs.next()) {
                                        System.out.printf("%d | %s | %d | %s%n",
                                                rs.getInt("d_id"),
                                                rs.getString("d_doctype"),
                                                rs.getInt("d_fee"),
                                                rs.getString("d_status"));
                                    }

                                } catch (SQLException e) {
                                    System.out.println("❌ Error viewing documents: " + e.getMessage());
                                }
                                break;

                            case 4: // Disable or Enable Document
                                try (Connection conn = connectDB();
                                        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tbl_doc");
                                        ResultSet rs = stmt.executeQuery()) {

                                    System.out.println("\n📜 Document List:");
                                    System.out.println("ID | Type | Fee | Status");
                                    System.out.println("----------------------------------");

                                    while (rs.next()) {
                                        System.out.printf("%d | %s | %d | %s%n",
                                                rs.getInt("d_id"),
                                                rs.getString("d_doctype"),
                                                rs.getInt("d_fee"),
                                                rs.getString("d_status"));
                                    }

                                    rs.close();
                                    stmt.close();

                                    System.out.print("\nEnter document ID to modify: ");
                                    int docId = sc.nextInt();
                                    sc.nextLine();

                                    // Check current status
                                    PreparedStatement checkStmt = conn.prepareStatement("SELECT d_status FROM tbl_doc WHERE d_id = ?");
                                    checkStmt.setInt(1, docId);
                                    ResultSet checkRs = checkStmt.executeQuery();

                                    if (!checkRs.next()) {
                                        System.out.println("❌ Document ID not found!");
                                        checkRs.close();
                                        checkStmt.close();
                                        break;
                                    }

                                    String currentStatus = checkRs.getString("d_status");
                                    checkRs.close();
                                    checkStmt.close();

                                    // Determine what to do next
                                    if (currentStatus.equalsIgnoreCase("Available")) {
                                        System.out.print("This document is currently AVAILABLE. Do you want to disable it? (Y/N): ");
                                        String choicee = sc.nextLine();

                                        if (choicee.equalsIgnoreCase("Y")) {
                                            PreparedStatement disableStmt = conn.prepareStatement(
                                                    "UPDATE tbl_doc SET d_status = 'Disabled' WHERE d_id = ?");
                                            disableStmt.setInt(1, docId);
                                            disableStmt.executeUpdate();
                                            disableStmt.close();
                                            System.out.println("✅ Document has been DISABLED.");
                                        } else {
                                            System.out.println("⚙️ Action cancelled.");
                                        }

                                    } else if (currentStatus.equalsIgnoreCase("Disabled")) {
                                        System.out.print("This document is currently DISABLED. Do you want to enable it? (Y/N): ");
                                        String choicee = sc.nextLine();

                                        if (choicee.equalsIgnoreCase("Y")) {
                                            PreparedStatement enableStmt = conn.prepareStatement(
                                                    "UPDATE tbl_doc SET d_status = 'Available' WHERE d_id = ?");
                                            enableStmt.setInt(1, docId);
                                            enableStmt.executeUpdate();
                                            enableStmt.close();
                                            System.out.println("Document has been ENABLED (set to Available).");
                                        } else {
                                            System.out.println("Action cancelled.");
                                        }

                                    } else {
                                        System.out.println("❌ Unknown status — cannot modify.");
                                    }

                                } catch (SQLException e) {
                                    System.out.println("Error modifying document: " + e.getMessage());
                                }
                                break;

                            case 5:
                                System.out.println("🔙 Returning to Admin Panel...");
                                break;

                            default:
                                System.out.println("❌ Invalid choice, please try again.");
                        }

                    } while (docChoice != 5);
                    break;

                case 9: {
                    System.out.println("\n==== All Document Request Logs ====");
                    String[] logHeaders = {"Request ID", "User ID", "Document Type", "Fee", "Purpose", "Date", "Status", "Approved By"};
                    String[] logCols = {"r_id", "u_id", "d_doctype", "r_fee", "r_purpose", "r_date", "r_status", "r_approvalby"};

                    String logQuery = "SELECT r_id, u_id, d_doctype, r_fee, r_purpose, r_date, r_status, "
                            + "COALESCE(r_approvalby, 'Pending') AS r_approvalby "
                            + "FROM tbl_req";

                    db.viewRecords(logQuery, logHeaders, logCols);
                    break;
                }

                case 10:
                    System.out.println("👋 You have been logged out successfully!");
                    return; // exits the program or returns to login menu

            }
        }
    }

    public static void showResidentMenu(Scanner sc, int userId) {
        while (true) {
            System.out.println("\n==== Resident Panel ====");
            System.out.println("1. Request Document");
            System.out.println("2. View My Request History and Info");
            System.out.println("3. Follow up/Cancel Request");
            System.out.println("4. Change Details and Password");
            System.out.println("5. Log Out");
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
                    followUpOrCancelRequest(sc, userId);
                    break;
                case 4:
                    changeDetailsAndPassword(sc, userId);
                    break;
                case 5:
                    System.out.println("Logging out...");
                    return; // exits the Resident Panel
                default:
                    System.out.println("❌ Invalid option. Try again.");
            }
        }
    }

    // ============================================
    // FOLLOW UP / CANCEL REQUEST FUNCTION
    // ============================================
    public static void followUpOrCancelRequest(Scanner sc, int userId) {
        System.out.println("\n==== My Document Requests ====");

        String sql = "SELECT r_id, d_doctype, r_purpose, r_fee, r_status, r_date FROM tbl_req WHERE u_id = ?";
        try (Connection conn = connectDB();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            List<Integer> requestIds = new ArrayList<>();
            int index = 1;

            while (rs.next()) {
                int reqId = rs.getInt("r_id");
                requestIds.add(reqId);
                System.out.printf("%d. %s | Purpose: %s | Fee: ₱%d | Status: %s | Date: %s%n",
                        index,
                        rs.getString("d_doctype"),
                        rs.getString("r_purpose"),
                        rs.getInt("r_fee"),
                        rs.getString("r_status"),
                        rs.getString("r_date"));
                index++;
            }

            if (requestIds.isEmpty()) {
                System.out.println("❌ You have no requests yet.");
                return;
            }

            System.out.println(index + ". Go Back");
            System.out.print("Select a request to cancel or follow up: ");
            int choice = sc.nextInt();
            sc.nextLine();

            if (choice == index) {
                return;
            }
            if (choice < 1 || choice > requestIds.size()) {
                System.out.println("❌ Invalid option.");
                return;
            }

            int selectedRequestId = requestIds.get(choice - 1);

            System.out.print("Do you want to cancel this request? (yes/no): ");
            String confirm = sc.nextLine().trim();

            if (confirm.equalsIgnoreCase("yes")) {
                String cancelSql = "UPDATE tbl_req SET r_status = 'Cancelled' WHERE r_id = ?";
                try (PreparedStatement cancelStmt = conn.prepareStatement(cancelSql)) {
                    cancelStmt.setInt(1, selectedRequestId);
                    cancelStmt.executeUpdate();
                    System.out.println("✅ Request has been cancelled.");
                }
            } else {
                System.out.println("ℹ️ Request not cancelled. You can follow up manually with admin if needed.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Error fetching requests: " + e.getMessage());
        }
    }

    public static void changeDetailsAndPassword(Scanner sc, int userId) {
        System.out.println("\n==== Change Details and Password ====");

        try (Connection conn = connectDB()) {
            // Show current details first
            String viewSql = "SELECT u_fullname, u_contact, u_password FROM tbl_user WHERE u_id = ?";
            PreparedStatement viewStmt = conn.prepareStatement(viewSql);
            viewStmt.setInt(1, userId);
            ResultSet rs = viewStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("❌ User not found!");
                return;
            }

            String currentName = rs.getString("u_fullname");
            String currentContact = rs.getString("u_contact");
            String currentPassword = rs.getString("u_password");

            System.out.println("Current Full Name: " + currentName);
            System.out.println("Current Contact: " + currentContact);
            System.out.println("-----------------------------------");

            // Ask for new details
            System.out.print("Enter new full name (leave blank to keep current): ");
            String newName = sc.nextLine().trim();
            if (newName.isEmpty()) {
                newName = currentName;
            }

            System.out.print("Enter new contact number (leave blank to keep current): ");
            String newContact = sc.nextLine().trim();
            if (newContact.isEmpty()) {
                newContact = currentContact;
            }

            System.out.print("Enter new password (leave blank to keep current): ");
            String newPassword = sc.nextLine().trim();
            if (newPassword.isEmpty()) {
                newPassword = currentPassword;
            }

            // Confirm
            System.out.print("Save changes? (yes/no): ");
            String confirm = sc.nextLine().trim();
            if (!confirm.equalsIgnoreCase("yes")) {
                System.out.println("❌ Changes cancelled.");
                return;
            }

            // Update database
            String updateSql = "UPDATE tbl_user SET u_fullname = ?, u_contact = ?, u_password = ? WHERE u_id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, newName);
            updateStmt.setString(2, newContact);
            updateStmt.setString(3, newPassword);
            updateStmt.setInt(4, userId);
            updateStmt.executeUpdate();

            System.out.println("✅ Details updated successfully!");

        } catch (SQLException e) {
            System.out.println("❌ Error updating details: " + e.getMessage());
        }
    }

    public static void viewProfile(int userId) {
        String sql = "SELECT u_id, u_name, u_fullname, u_role, u_contact, u_status FROM tbl_user WHERE u_id = " + userId;
        String[] headers = {"ID", "Username", "Full Name", "Role", "Contact", "Status"};
        String[] cols = {"u_id", "u_name", "u_fullname", "u_role", "u_contact", "u_status"};
        db.viewRecords(sql, headers, cols);
    }

    private static void requestDocument(Scanner sc, int userId) {
        System.out.println("\n==== Request Document ====");

        String sql = "SELECT d_id, d_doctype, d_fee FROM tbl_doc";
        try (Connection conn = connectDB();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            List<Integer> docIds = new ArrayList<>();
            List<String> docTypes = new ArrayList<>();
            List<Integer> docFees = new ArrayList<>();

            int index = 1;
            while (rs.next()) {
                docIds.add(rs.getInt("d_id"));
                docTypes.add(rs.getString("d_doctype"));
                docFees.add(rs.getInt("d_fee"));
                System.out.println(index + ". " + rs.getString("d_doctype") + " (₱" + rs.getInt("d_fee") + ")");
                index++;
            }

            if (docIds.isEmpty()) {
                System.out.println("❌ No documents available to request.");
                return;
            }

            System.out.println(index + ". Go Back");
            System.out.print("Choose a document: ");
            int choice = sc.nextInt();
            sc.nextLine();

            if (choice == index) {
                return; // Go back
            }
            if (choice < 1 || choice > docIds.size()) {
                System.out.println("❌ Invalid option.");
                return;
            }

            String selectedType = docTypes.get(choice - 1);
            int selectedFee = docFees.get(choice - 1);
            int selectedDocId = docIds.get(choice - 1);

            System.out.println("\nYou selected: " + selectedType + " (₱" + selectedFee + ")");
            System.out.print("Enter purpose / reason: ");
            String purpose = sc.nextLine().trim();

            System.out.print("Confirm submit request? (yes/no): ");
            String confirm = sc.nextLine().trim();

            if (!confirm.equalsIgnoreCase("yes")) {
                System.out.println("❌ Request cancelled.");
                return;
            }

            String insertSql = "INSERT INTO tbl_req (u_id, d_id, d_doctype, r_fee, r_purpose, r_status, r_date) "
                    + "VALUES (?, ?, ?, ?, ?, 'Pending', datetime('now'))";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, selectedDocId);
                insertStmt.setString(3, selectedType);
                insertStmt.setInt(4, selectedFee);
                insertStmt.setString(5, purpose);
                insertStmt.executeUpdate();
                System.out.println("✅ Document request submitted successfully!");
            }

        } catch (SQLException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

}
