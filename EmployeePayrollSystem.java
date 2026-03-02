import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class EmployeePayrollSystem {
    private static Scanner scanner = new Scanner(System.in);
    private static List<Employee> employees = new ArrayList<>();
    private static List<Payroll> payrolls = new ArrayList<>();
    private static final String EMPLOYEE_FILE = "employees.dat";
    private static final String PAYROLL_FILE = "payrolls.dat";
    
    public static void main(String[] args) {
        loadData();
        while (true) {
            displayMainMenu();
            int choice = getIntInput("Enter your choice: ");
            
            switch (choice) {
                case 1: addEmployee(); break;
                case 2: viewAllEmployees(); break;
                case 3: processPayroll(); break;
                case 4: viewPayrollHistory(); break;
                case 5: generateReport(); break;
                case 6: searchEmployee(); break;
                case 7: updateEmployee(); break;
                case 8: deleteEmployee(); break;
                case 9: 
                    saveData();
                    System.out.println("Thank you for using Employee Payroll System!");
                    return;
                default: System.out.println("Invalid choice! Please try again.");
            }
        }
    }

    private static void displayMainMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("     EMPLOYEE PAYROLL MANAGEMENT SYSTEM");
        System.out.println("=".repeat(50));
        System.out.println("1. Add New Employee");
        System.out.println("2. View All Employees");
        System.out.println("3. Process Payroll");
        System.out.println("4. View Payroll History");
        System.out.println("5. Generate Salary Report");
        System.out.println("6. Search Employee");
        System.out.println("7. Update Employee Information");
        System.out.println("8. Delete Employee");
        System.out.println("9. Exit");
        System.out.println("-".repeat(50));
    }

    private static void addEmployee() {
        System.out.println("\n--- ADD NEW EMPLOYEE ---");
        System.out.println("Employee Types:");
        System.out.println("1. Full Time");
        System.out.println("2. Part Time");
        System.out.println("3. Contractor");
        
        int type = getIntInput("Select employee type: ");
        
        String id = getStringInput("Enter Employee ID: ");
        if (findEmployeeById(id) != null) {
            System.out.println("Employee ID already exists!");
            return;
        }
        
        String name = getStringInput("Enter Name: ");
        String department = getStringInput("Enter Department: ");
        LocalDate hireDate = getDateInput("Enter Hire Date (YYYY-MM-DD): ");
        
        switch (type) {
            case 1:
                double salary = getDoubleInput("Enter Annual Salary: $");
                employees.add(new FullTimeEmployee(id, name, department, hireDate, salary));
                System.out.println("✅ Full-time employee added successfully!");
                break;
            case 2:
                double hourlyRate = getDoubleInput("Enter Hourly Rate: $");
                employees.add(new PartTimeEmployee(id, name, department, hireDate, hourlyRate));
                System.out.println("✅ Part-time employee added successfully!");
                break;
            case 3:
                double contractAmount = getDoubleInput("Enter Contract Amount: $");
                int duration = getIntInput("Enter Contract Duration (months): ");
                employees.add(new Contractor(id, name, department, hireDate, contractAmount, duration));
                System.out.println("✅ Contractor added successfully!");
                break;
            default:
                System.out.println("Invalid employee type!");
        }
        saveData();
    }

    private static void viewAllEmployees() {
        System.out.println("\n--- ALL EMPLOYEES ---");
        if (employees.isEmpty()) {
            System.out.println("No employees found.");
            return;
        }
        
        System.out.println("\n" + "-".repeat(80));
        System.out.printf("%-10s %-20s %-15s %-12s %-15s%n", 
            "ID", "Name", "Department", "Type", "Salary/Pay");
        System.out.println("-".repeat(80));
        
        for (Employee emp : employees) {
            String type = "";
            String pay = "";
            
            if (emp instanceof FullTimeEmployee) {
                type = "Full-Time";
                pay = String.format("$%.2f/yr", emp.getBaseSalary());
            } else if (emp instanceof PartTimeEmployee) {
                type = "Part-Time";
                pay = String.format("$%.2f/hr", ((PartTimeEmployee) emp).getHourlyRate());
            } else if (emp instanceof Contractor) {
                type = "Contractor";
                Contractor c = (Contractor) emp;
                pay = String.format("$%.2f/%d months", c.getContractAmount(), c.getContractDuration());
            }
            
            System.out.printf("%-10s %-20s %-15s %-12s %-15s%n",
                emp.getEmployeeId(), emp.getName(), emp.getDepartment(), type, pay);
        }
        System.out.println("-".repeat(80));
        System.out.println("Total Employees: " + employees.size());
    }

    private static void processPayroll() {
        System.out.println("\n--- PROCESS PAYROLL ---");
        String id = getStringInput("Enter Employee ID: ");
        
        Employee emp = findEmployeeById(id);
        if (emp == null) {
            System.out.println("Employee not found!");
            return;
        }
        
        // Show employee details
        System.out.println("\nEmployee: " + emp.getName() + " (" + getEmployeeType(emp) + ")");
        
        // Get pay period
        String period = getStringInput("Enter Pay Period (YYYY-MM): ");
        LocalDate payPeriod;
        try {
            payPeriod = YearMonth.parse(period).atDay(1);
        } catch (Exception e) {
            System.out.println("Invalid date format!");
            return;
        }
        
        // Check if already processed
        if (isPayrollProcessed(id, payPeriod)) {
            System.out.println("Payroll already processed for this period!");
            return;
        }
        
        // Get additional info based on employee type
        if (emp instanceof PartTimeEmployee) {
            int hours = getIntInput("Enter hours worked: ");
            ((PartTimeEmployee) emp).setHoursWorked(hours);
        } else if (emp instanceof FullTimeEmployee) {
            System.out.print("Any bonus? (y/n): ");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                double bonus = getDoubleInput("Enter bonus amount: $");
                ((FullTimeEmployee) emp).addBonus(bonus);
            }
            
            System.out.print("Any deductions? (y/n): ");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                double deduction = getDoubleInput("Enter deduction amount: $");
                ((FullTimeEmployee) emp).addDeduction(deduction);
            }
        }
        
        // Calculate payroll
        double grossPay = emp.calculateSalary();
        double tax = calculateTax(emp, grossPay);
        double netPay = grossPay - tax;
        
        // Display summary
        System.out.println("\n" + "=".repeat(40));
        System.out.println("PAYROLL SUMMARY");
        System.out.println("=".repeat(40));
        System.out.printf("Employee: %s (%s)%n", emp.getName(), emp.getEmployeeId());
        System.out.printf("Period: %s%n", payPeriod.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        System.out.printf("Gross Pay: $%.2f%n", grossPay);
        System.out.printf("Tax Deduction: $%.2f%n", tax);
        System.out.printf("Net Pay: $%.2f%n", netPay);
        System.out.println("=".repeat(40));
        
        System.out.print("\nProcess this payroll? (y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            String payrollId = "PAY-" + id + "-" + period.replace("-", "");
            Payroll payroll = new Payroll(payrollId, emp, payPeriod, grossPay, tax, netPay);
            payrolls.add(payroll);
            
            // Reset hours for part-time employees
            if (emp instanceof PartTimeEmployee) {
                ((PartTimeEmployee) emp).resetHours();
            }
            
            System.out.println("✅ Payroll processed successfully!");
            saveData();
        } else {
            System.out.println("Payroll processing cancelled.");
        }
    }

    private static void viewPayrollHistory() {
        System.out.println("\n--- PAYROLL HISTORY ---");
        if (payrolls.isEmpty()) {
            System.out.println("No payroll records found.");
            return;
        }
        
        System.out.print("Filter by Employee ID (or 'all' for all): ");
        String filter = scanner.nextLine();
        
        System.out.println("\n" + "-".repeat(90));
        System.out.printf("%-15s %-20s %-15s %-12s %-12s %-12s%n", 
            "Payroll ID", "Employee", "Period", "Gross", "Tax", "Net");
        System.out.println("-".repeat(90));
        
        double totalGross = 0, totalNet = 0, totalTax = 0;
        int count = 0;
        
        for (Payroll p : payrolls) {
            if (filter.equalsIgnoreCase("all") || p.getEmployeeId().equalsIgnoreCase(filter)) {
                System.out.printf("%-15s %-20s %-15s $%-10.2f $%-10.2f $%-10.2f%n",
                    p.getPayrollId(), 
                    truncate(p.getEmployeeName(), 20),
                    p.getFormattedPeriod(),
                    p.getGrossPay(), p.getTax(), p.getNetPay());
                
                totalGross += p.getGrossPay();
                totalTax += p.getTax();
                totalNet += p.getNetPay();
                count++;
            }
        }
        
        if (count > 0) {
            System.out.println("-".repeat(90));
            System.out.printf("%-52s $%-10.2f $%-10.2f $%-10.2f%n", 
                "TOTAL (" + count + " records):", totalGross, totalTax, totalNet);
        } else {
            System.out.println("No records found.");
        }
    }

    private static void generateReport() {
        System.out.println("\n--- SALARY REPORT ---");
        System.out.println("1. Monthly Report");
        System.out.println("2. Department-wise Report");
        System.out.println("3. Employee Type Report");
        
        int choice = getIntInput("Select report type: ");
        
        switch (choice) {
            case 1:
                generateMonthlyReport();
                break;
            case 2:
                generateDepartmentReport();
                break;
            case 3:
                generateTypeReport();
                break;
            default:
                System.out.println("Invalid choice!");
        }
    }

    private static void generateMonthlyReport() {
        String month = getStringInput("Enter month (YYYY-MM): ");
        LocalDate period;
        try {
            period = YearMonth.parse(month).atDay(1);
        } catch (Exception e) {
            System.out.println("Invalid month format!");
            return;
        }
        
        System.out.println("\n📊 MONTHLY SALARY REPORT - " + 
            period.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        System.out.println("=".repeat(60));
        
        double totalGross = 0, totalNet = 0, totalTax = 0;
        int count = 0;
        
        for (Payroll p : payrolls) {
            if (p.getPayPeriod().equals(period)) {
                System.out.printf("%-20s | Gross: $%-8.2f | Tax: $%-6.2f | Net: $%-8.2f%n",
                    p.getEmployeeName(), p.getGrossPay(), p.getTax(), p.getNetPay());
                totalGross += p.getGrossPay();
                totalTax += p.getTax();
                totalNet += p.getNetPay();
                count++;
            }
        }
        
        if (count > 0) {
            System.out.println("=".repeat(60));
            System.out.printf("SUMMARY (%d employees):%n", count);
            System.out.printf("Total Gross Pay: $%.2f%n", totalGross);
            System.out.printf("Total Tax: $%.2f%n", totalTax);
            System.out.printf("Total Net Pay: $%.2f%n", totalNet);
            System.out.printf("Average Net Pay: $%.2f%n", totalNet / count);
        } else {
            System.out.println("No payroll records for this month.");
        }
    }

    private static void generateDepartmentReport() {
        Map<String, Double> deptTotal = new HashMap<>();
        Map<String, Integer> deptCount = new HashMap<>();
        
        for (Payroll p : payrolls) {
            String dept = p.getDepartment();
            deptTotal.put(dept, deptTotal.getOrDefault(dept, 0.0) + p.getNetPay());
            deptCount.put(dept, deptCount.getOrDefault(dept, 0) + 1);
        }
        
        System.out.println("\n📊 DEPARTMENT WISE REPORT");
        System.out.println("=".repeat(50));
        System.out.printf("%-20s %-15s %-15s%n", "Department", "Employees", "Total Payout");
        System.out.println("-".repeat(50));
        
        for (String dept : deptTotal.keySet()) {
            System.out.printf("%-20s %-15d $%-14.2f%n", 
                dept, deptCount.get(dept), deptTotal.get(dept));
        }
    }

    private static void generateTypeReport() {
        Map<String, Double> typeTotal = new HashMap<>();
        Map<String, Integer> typeCount = new HashMap<>();
        
        for (Employee emp : employees) {
            String type = getEmployeeType(emp);
            double salary = emp.calculateSalary();
            typeTotal.put(type, typeTotal.getOrDefault(type, 0.0) + salary);
            typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
        }
        
        System.out.println("\n📊 EMPLOYEE TYPE REPORT");
        System.out.println("=".repeat(50));
        System.out.printf("%-15s %-10s %-15s%n", "Type", "Count", "Avg Monthly Pay");
        System.out.println("-".repeat(50));
        
        for (String type : typeTotal.keySet()) {
            double avg = typeTotal.get(type) / typeCount.get(type);
            System.out.printf("%-15s %-10d $%-14.2f%n", type, typeCount.get(type), avg);
        }
    }

    private static void searchEmployee() {
        System.out.println("\n--- SEARCH EMPLOYEE ---");
        System.out.println("1. Search by ID");
        System.out.println("2. Search by Name");
        System.out.println("3. Search by Department");
        
        int choice = getIntInput("Select search option: ");
        
        switch (choice) {
            case 1:
                String id = getStringInput("Enter Employee ID: ");
                Employee emp = findEmployeeById(id);
                if (emp != null) {
                    displayEmployeeDetails(emp);
                } else {
                    System.out.println("Employee not found!");
                }
                break;
                
            case 2:
                String name = getStringInput("Enter Name: ").toLowerCase();
                boolean found = false;
                for (Employee e : employees) {
                    if (e.getName().toLowerCase().contains(name)) {
                        displayEmployeeDetails(e);
                        found = true;
                    }
                }
                if (!found) System.out.println("No employees found with that name.");
                break;
                
            case 3:
                String dept = getStringInput("Enter Department: ").toLowerCase();
                found = false;
                for (Employee e : employees) {
                    if (e.getDepartment().toLowerCase().contains(dept)) {
                        displayEmployeeDetails(e);
                        found = true;
                    }
                }
                if (!found) System.out.println("No employees found in that department.");
                break;
        }
    }

    private static void updateEmployee() {
        System.out.println("\n--- UPDATE EMPLOYEE ---");
        String id = getStringInput("Enter Employee ID: ");
        
        Employee emp = findEmployeeById(id);
        if (emp == null) {
            System.out.println("Employee not found!");
            return;
        }
        
        displayEmployeeDetails(emp);
        
        System.out.println("\nWhat would you like to update?");
        System.out.println("1. Name");
        System.out.println("2. Department");
        System.out.println("3. Salary/Rate");
        System.out.println("4. Contact Information");
        
        int choice = getIntInput("Select option: ");
        
        switch (choice) {
            case 1:
                emp.setName(getStringInput("Enter new name: "));
                System.out.println("Name updated!");
                break;
            case 2:
                emp.setDepartment(getStringInput("Enter new department: "));
                System.out.println("Department updated!");
                break;
            case 3:
                if (emp instanceof FullTimeEmployee) {
                    double salary = getDoubleInput("Enter new annual salary: $");
                    emp.setBaseSalary(salary);
                } else if (emp instanceof PartTimeEmployee) {
                    double rate = getDoubleInput("Enter new hourly rate: $");
                    ((PartTimeEmployee) emp).setHourlyRate(rate);
                } else if (emp instanceof Contractor) {
                    double amount = getDoubleInput("Enter new contract amount: $");
                    ((Contractor) emp).setContractAmount(amount);
                }
                System.out.println("Salary information updated!");
                break;
            case 4:
                emp.setEmail(getStringInput("Enter email: "));
                emp.setPhoneNumber(getStringInput("Enter phone number: "));
                System.out.println("Contact information updated!");
                break;
            default:
                System.out.println("Invalid option!");
        }
        saveData();
    }

    private static void deleteEmployee() {
        System.out.println("\n--- DELETE EMPLOYEE ---");
        String id = getStringInput("Enter Employee ID: ");
        
        Employee emp = findEmployeeById(id);
        if (emp == null) {
            System.out.println("Employee not found!");
            return;
        }
        
        displayEmployeeDetails(emp);
        
        System.out.print("\nAre you sure you want to delete this employee? (y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            employees.remove(emp);
            System.out.println("Employee deleted successfully!");
            saveData();
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private static void displayEmployeeDetails(Employee emp) {
        System.out.println("\n" + "=".repeat(40));
        System.out.println("EMPLOYEE DETAILS");
        System.out.println("=".repeat(40));
        System.out.println("ID: " + emp.getEmployeeId());
        System.out.println("Name: " + emp.getName());
        System.out.println("Department: " + emp.getDepartment());
        System.out.println("Hire Date: " + emp.getHireDate());
        System.out.println("Type: " + getEmployeeType(emp));
        
        if (emp instanceof FullTimeEmployee) {
            FullTimeEmployee ft = (FullTimeEmployee) emp;
            System.out.printf("Annual Salary: $%.2f%n", ft.getBaseSalary());
            System.out.printf("Monthly Salary: $%.2f%n", ft.getBaseSalary() / 12);
            System.out.println("Paid Leaves: " + ft.getPaidLeaves());
        } else if (emp instanceof PartTimeEmployee) {
            PartTimeEmployee pt = (PartTimeEmployee) emp;
            System.out.printf("Hourly Rate: $%.2f%n", pt.getHourlyRate());
            System.out.println("Hours Worked: " + pt.getHoursWorked());
        } else if (emp instanceof Contractor) {
            Contractor c = (Contractor) emp;
            System.out.printf("Contract Amount: $%.2f%n", c.getContractAmount());
            System.out.println("Contract Duration: " + c.getContractDuration() + " months");
            System.out.println("Months Completed: " + c.getMonthsCompleted());
        }
        
        if (emp.getEmail() != null) System.out.println("Email: " + emp.getEmail());
        if (emp.getPhoneNumber() != null) System.out.println("Phone: " + emp.getPhoneNumber());
        System.out.println("=".repeat(40));
    }

    private static String getEmployeeType(Employee emp) {
        if (emp instanceof FullTimeEmployee) return "Full-Time";
        if (emp instanceof PartTimeEmployee) return "Part-Time";
        if (emp instanceof Contractor) return "Contractor";
        return "Unknown";
    }

    private static double calculateTax(Employee emp, double grossPay) {
        if (emp instanceof FullTimeEmployee) return grossPay * 0.20;
        if (emp instanceof PartTimeEmployee) return grossPay * 0.15;
        if (emp instanceof Contractor) return grossPay * 0.10;
        return 0;
    }

    private static Employee findEmployeeById(String id) {
        for (Employee emp : employees) {
            if (emp.getEmployeeId().equalsIgnoreCase(id)) {
                return emp;
            }
        }
        return null;
    }

    private static boolean isPayrollProcessed(String empId, LocalDate period) {
        for (Payroll p : payrolls) {
            if (p.getEmployeeId().equalsIgnoreCase(empId) && p.getPayPeriod().equals(period)) {
                return true;
            }
        }
        return false;
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number!");
            }
        }
    }

    private static double getDoubleInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number!");
            }
        }
    }

    private static LocalDate getDateInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.println("Please enter a valid date (YYYY-MM-DD)!");
            }
        }
    }

    private static String truncate(String str, int length) {
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

    @SuppressWarnings("unchecked")
    private static void loadData() {
        try {
            File empFile = new File(EMPLOYEE_FILE);
            if (empFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(empFile));
                employees = (ArrayList<Employee>) ois.readObject();
                ois.close();
            }
            
            File payFile = new File(PAYROLL_FILE);
            if (payFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(payFile));
                payrolls = (ArrayList<Payroll>) ois.readObject();
                ois.close();
            }
            
            System.out.println("Data loaded successfully!");
        } catch (Exception e) {
            System.out.println("No existing data found. Starting fresh.");
        }
    }

    private static void saveData() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(EMPLOYEE_FILE));
            oos.writeObject(employees);
            oos.close();
            
            oos = new ObjectOutputStream(new FileOutputStream(PAYROLL_FILE));
            oos.writeObject(payrolls);
            oos.close();
            
            System.out.println("Data saved successfully!");
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }
}

// Employee Classes
abstract class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String employeeId;
    protected String name;
    protected String department;
    protected LocalDate hireDate;
    protected double baseSalary;
    protected String email;
    protected String phoneNumber;
    
    public Employee(String employeeId, String name, String department, 
                   LocalDate hireDate, double baseSalary) {
        this.employeeId = employeeId;
        this.name = name;
        this.department = department;
        this.hireDate = hireDate;
        this.baseSalary = baseSalary;
    }
    
    public abstract double calculateSalary();
    
    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public LocalDate getHireDate() { return hireDate; }
    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}

class FullTimeEmployee extends Employee {
    private static final long serialVersionUID = 1L;
    private double bonus;
    private double deductions;
    private int paidLeaves;
    private int unpaidLeaves;
    
    public FullTimeEmployee(String employeeId, String name, String department,
                           LocalDate hireDate, double baseSalary) {
        super(employeeId, name, department, hireDate, baseSalary);
        this.bonus = 0;
        this.deductions = 0;
        this.paidLeaves = 20;
        this.unpaidLeaves = 0;
    }
    
    @Override
    public double calculateSalary() {
        double monthlySalary = baseSalary / 12;
        double leaveDeduction = (unpaidLeaves * monthlySalary) / 30;
        return monthlySalary + bonus - deductions - leaveDeduction;
    }
    
    public void addBonus(double amount) { this.bonus += amount; }
    public void addDeduction(double amount) { this.deductions += amount; }
    public void takeLeave(int days, boolean isPaid) {
        if (isPaid && paidLeaves >= days) {
            paidLeaves -= days;
        } else {
            unpaidLeaves += days;
        }
    }
    public double getBonus() { return bonus; }
    public double getDeductions() { return deductions; }
    public int getPaidLeaves() { return paidLeaves; }
    public int getUnpaidLeaves() { return unpaidLeaves; }
}

class PartTimeEmployee extends Employee {
    private static final long serialVersionUID = 1L;
    private double hourlyRate;
    private int hoursWorked;
    
    public PartTimeEmployee(String employeeId, String name, String department,
                           LocalDate hireDate, double hourlyRate) {
        super(employeeId, name, department, hireDate, 0);
        this.hourlyRate = hourlyRate;
        this.hoursWorked = 0;
    }
    
    @Override
    public double calculateSalary() {
        return hoursWorked * hourlyRate;
    }
    
    public void addHours(int hours) { this.hoursWorked += hours; }
    public void resetHours() { this.hoursWorked = 0; }
    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }
    public int getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(int hoursWorked) { this.hoursWorked = hoursWorked; }
}

class Contractor extends Employee {
    private static final long serialVersionUID = 1L;
    private double contractAmount;
    private int contractDuration;
    private int monthsCompleted;
    
    public Contractor(String employeeId, String name, String department,
                     LocalDate hireDate, double contractAmount, int contractDuration) {
        super(employeeId, name, department, hireDate, 0);
        this.contractAmount = contractAmount;
        this.contractDuration = contractDuration;
        this.monthsCompleted = 0;
    }
    
    @Override
    public double calculateSalary() {
        return contractAmount / contractDuration;
    }
    
    public void completeMonth() {
        if (monthsCompleted < contractDuration) {
            monthsCompleted++;
        }
    }
    
    public double getContractAmount() { return contractAmount; }
    public void setContractAmount(double contractAmount) { this.contractAmount = contractAmount; }
    public int getContractDuration() { return contractDuration; }
    public int getMonthsCompleted() { return monthsCompleted; }
}

class Payroll implements Serializable {
    private static final long serialVersionUID = 1L;
    private String payrollId;
    private String employeeId;
    private String employeeName;
    private String department;
    private LocalDate payPeriod;
    private double grossPay;
    private double tax;
    private double netPay;
    
    public Payroll(String payrollId, Employee employee, LocalDate payPeriod, 
                   double grossPay, double tax, double netPay) {
        this.payrollId = payrollId;
        this.employeeId = employee.getEmployeeId();
        this.employeeName = employee.getName();
        this.department = employee.getDepartment();
        this.payPeriod = payPeriod;
        this.grossPay = grossPay;
        this.tax = tax;
        this.netPay = netPay;
    }
    
    public String getPayrollId() { return payrollId; }
    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getDepartment() { return department; }
    public LocalDate getPayPeriod() { return payPeriod; }
    public double getGrossPay() { return grossPay; }
    public double getTax() { return tax; }
    public double getNetPay() { return netPay; }
    public String getFormattedPeriod() {
        return payPeriod.format(DateTimeFormatter.ofPattern("MMM yyyy"));
    }
}
