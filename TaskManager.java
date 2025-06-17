import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors; // Import for stream operations

// Main class for the Task Management application
public class TaskManager {
    // Constant for the file name where tasks are stored
    static final String FILE_NAME = "tasks.csv";
    // Formatter for parsing and formatting dates in YYYY-MM-DD format
    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Inner class representing a single Task
    static class Task {
        int id;                // Unique identifier for the task
        String title;          // Title of the task
        String description;    // Detailed description of the task
        LocalDate dueDate;     // Due date of the task
        String priority;       // Priority of the task (e.g., High, Medium, Low)
        String status;         // Current status of the task (e.g., Pending, Completed, Overdue)
        LocalDateTime createdAt; // Timestamp when the task was created
        LocalDateTime completedAt; // Timestamp when the task was completed (null if not completed)

        // Constructor for the Task class
        Task(int id, String title, String description, LocalDate dueDate, String priority, String status, LocalDateTime createdAt, LocalDateTime completedAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.priority = priority;
            this.status = status;
            this.createdAt = createdAt;
            this.completedAt = completedAt;
        }

        // Converts the Task object to a CSV formatted string
        String toCSV() {
            // If completedAt is null, an empty string is used; otherwise, the full timestamp is used.
            return id + "," + title + "," + description + "," + dueDate + "," + priority + "," + status + "," + createdAt + "," + (completedAt != null ? completedAt : "");
        }
    }

    // Main method where the application execution begins
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); // Scanner to read user input
        List<Task> tasks = loadTasks(); // Load existing tasks from the CSV file

        // Update any overdue tasks at application start
        updateOverdueTasks(tasks);
        // Save tasks after updating overdue statuses (in case tasks became overdue on load)
        saveTasks(tasks);

        // Main application loop
        while (true) {
            System.out.println("\n--- Smart To-Do List ---");
            System.out.println("1. Add Task");
            System.out.println("2. Mark Task as Completed");
            System.out.println("3. Delete Task");
            System.out.println("4. View All Tasks");
            System.out.println("5. Filter Tasks"); // New menu option for filtering
            System.out.println("6. Exit");
            System.out.print("Choose option: ");

            int choice;
            try {
                // Read user's menu choice, handling non-numeric input gracefully
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue; // Continue to the next iteration of the loop
            }

            // Perform actions based on user's choice
            switch (choice) {
                case 1 -> addTask(tasks, scanner);
                case 2 -> markCompleted(tasks, scanner);
                case 3 -> deleteTask(tasks, scanner);
                case 4 -> viewTasks(tasks); // View all tasks
                case 5 -> filterTasks(tasks, scanner); // Call new filter method
                case 6 -> { // Exit option moved to 6
                    saveTasks(tasks); // Save all tasks before exiting
                    System.out.println("Exiting...");
                    scanner.close(); // Close the scanner to release resources
                    return; // Exit the application
                }
                default -> System.out.println("Invalid option! Please choose a number between 1 and 6.");
            }
        }
    }

    // Method to add a new task to the list
    static void addTask(List<Task> tasks, Scanner sc) {
        // Generate a new unique ID for the task
        // If the list is empty, start ID from 1, otherwise increment the last task's ID
        int id = tasks.isEmpty() ? 1 : tasks.get(tasks.size() - 1).id + 1;

        System.out.print("Title: ");
        String title = sc.nextLine().trim(); // Trim whitespace from input
        System.out.print("Description: ");
        String desc = sc.nextLine().trim();

        LocalDate due;
        while (true) {
            System.out.print("Due Date (YYYY-MM-DD): ");
            String dateStr = sc.nextLine().trim();
            try {
                // Parse the due date, handling invalid date format gracefully
                due = LocalDate.parse(dateStr, formatter);
                break; // Exit loop if date is parsed successfully
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        String priority;
        while (true) {
            System.out.print("Priority (High/Medium/Low): ");
            priority = sc.nextLine().trim();
            // Validate priority input (case-insensitive)
            if (priority.equalsIgnoreCase("High") || priority.equalsIgnoreCase("Medium") || priority.equalsIgnoreCase("Low")) {
                break; // Exit loop if priority is valid
            } else {
                System.out.println("Invalid priority. Please enter High, Medium, or Low.");
            }
        }

        // Create a new Task object with "Pending" status and current creation timestamp
        Task task = new Task(id, title, desc, due, priority, "Pending", LocalDateTime.now(), null);
        tasks.add(task); // Add the new task to the list
        System.out.println("Task added successfully!");
        saveTasks(tasks); // Save tasks immediately after adding to persist changes
    }

    // Method to mark a task as completed
    static void markCompleted(List<Task> tasks, Scanner sc) {
        System.out.print("Enter Task ID to mark completed: ");
        int id;
        try {
            id = Integer.parseInt(sc.nextLine().trim()); // Read task ID, handling non-numeric input
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a numeric Task ID.");
            return; // Exit method if input is invalid
        }

        boolean found = false;
        for (Task t : tasks) {
            // Find the task by ID and ensure it's not already completed
            if (t.id == id) {
                if (!t.status.equals("Completed")) {
                    t.status = "Completed"; // Update status
                    t.completedAt = LocalDateTime.now(); // Set completion timestamp
                    System.out.println("Task ID " + id + " marked as completed!");
                    found = true;
                } else {
                    System.out.println("Task ID " + id + " is already completed.");
                    found = true; // Still found, but already completed
                }
                break; // Exit loop once task is found
            }
        }
        if (!found) {
            System.out.println("Task not found with ID " + id + ".");
        }
        saveTasks(tasks); // Save tasks immediately after marking completed
    }

    // Method to delete a task from the list
    static void deleteTask(List<Task> tasks, Scanner sc) {
        System.out.print("Enter Task ID to delete: ");
        int id;
        try {
            id = Integer.parseInt(sc.nextLine().trim()); // Read task ID, handling non-numeric input
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a numeric Task ID.");
            return; // Exit method if input is invalid
        }

        // Remove the task if its ID matches, and capture if any task was removed
        boolean removed = tasks.removeIf(t -> t.id == id);
        if (removed) {
            System.out.println("Task with ID " + id + " deleted.");
        } else {
            System.out.println("Task with ID " + id + " not found.");
        }
        saveTasks(tasks); // Save tasks immediately after deleting
    }

    // Method to view all tasks in a formatted table
    // This now accepts a list of tasks to display, allowing it to be used for filtered views
    static void viewTasks(List<Task> tasksToDisplay) {
        // Ensure overdue tasks are updated before displaying
        updateOverdueTasks(tasksToDisplay);

        if (tasksToDisplay.isEmpty()) {
            System.out.println("No tasks to display.");
            return;
        }
        System.out.printf("\n%-4s %-20s %-10s %-12s %-10s %-12s\n", "ID", "Title", "Priority", "DueDate", "Status", "CreatedAt");
        System.out.println("-----------------------------------------------------------------------");

        // Custom comparator for sorting tasks: Overdue > High > Medium > Low > Pending > Completed (alphabetical)
        tasksToDisplay.sort((t1, t2) -> {
            Map<String, Integer> priorityOrder = new HashMap<>();
            priorityOrder.put("Overdue", 0);   // Highest priority
            priorityOrder.put("High", 1);
            priorityOrder.put("Medium", 2);
            priorityOrder.put("Low", 3);
            priorityOrder.put("Pending", 4);   // Lower than defined priorities, but before Completed
            priorityOrder.put("Completed", 5); // Lowest priority

            int p1 = priorityOrder.getOrDefault(t1.status.equals("Overdue") ? "Overdue" : t1.priority, 99); // Use Overdue status for priority
            int p2 = priorityOrder.getOrDefault(t2.status.equals("Overdue") ? "Overdue" : t2.priority, 99);

            // If tasks have different effective priorities (including Overdue status)
            int priorityCompare = Integer.compare(p1, p2);
            if (priorityCompare != 0) {
                return priorityCompare;
            }

            // Secondary sort by due date for tasks with the same priority (earlier due date first)
            return t1.dueDate.compareTo(t2.dueDate);
        });

        // Print each task's details
        for (Task t : tasksToDisplay) {
            System.out.printf("%-4d %-20s %-10s %-12s %-10s %-12s\n",
                    t.id,
                    (t.title.length() > 18 ? t.title.substring(0, 15) + "..." : t.title), // Truncate title for display
                    t.priority,
                    t.dueDate.format(formatter), // Format due date for display
                    t.status,
                    t.createdAt.toLocalDate().format(formatter)); // Format creation date for display
        }
        System.out.println("-----------------------------------------------------------------------");
    }

    // NEW Method to filter tasks
    static void filterTasks(List<Task> allTasks, Scanner sc) {
        while (true) {
            System.out.println("\n--- Filter Tasks ---");
            System.out.println("1. View Pending Tasks");
            System.out.println("2. View Completed Tasks");
            System.out.println("3. View Tasks Due Today or Tomorrow");
            System.out.println("4. View Overdue Tasks");
            System.out.println("5. Back to Main Menu");
            System.out.print("Choose filter option: ");

            int filterChoice;
            try {
                filterChoice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number (1-5).");
                continue;
            }

            List<Task> filteredList = new ArrayList<>();
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            switch (filterChoice) {
                case 1 -> { // View Pending Tasks
                    filteredList = allTasks.stream()
                            .filter(t -> t.status.equals("Pending"))
                            .collect(Collectors.toList());
                    System.out.println("\n--- Pending Tasks ---");
                    viewTasks(filteredList); // Use the existing viewTasks for display
                }
                case 2 -> { // View Completed Tasks
                    filteredList = allTasks.stream()
                            .filter(t -> t.status.equals("Completed"))
                            .collect(Collectors.toList());
                    System.out.println("\n--- Completed Tasks ---");
                    viewTasks(filteredList);
                }
                case 3 -> { // View Tasks Due Today or Tomorrow
                    filteredList = allTasks.stream()
                            .filter(t -> t.dueDate.equals(today) || t.dueDate.equals(tomorrow))
                            .collect(Collectors.toList());
                    System.out.println("\n--- Tasks Due Today or Tomorrow ---");
                    viewTasks(filteredList);
                }
                case 4 -> { // View Overdue Tasks
                    filteredList = allTasks.stream()
                            .filter(t -> t.status.equals("Overdue")) // Overdue status is already updated on load/view
                            .collect(Collectors.toList());
                    System.out.println("\n--- Overdue Tasks ---");
                    viewTasks(filteredList);
                }
                case 5 -> { // Back to Main Menu
                    return; // Exit filter menu
                }
                default -> System.out.println("Invalid filter option! Please choose a number between 1 and 5.");
            }
        }
    }


    // Method to load tasks from the CSV file
    static List<Task> loadTasks() {
        List<Task> list = new ArrayList<>();
        File file = new File(FILE_NAME);
        // If the file does not exist, return an empty list
        if (!file.exists()) {
            System.out.println("No existing tasks file found. Starting with an empty list.");
            return list;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // Read and skip the header line
            if (line == null) { // Handle case where file is empty except for header
                return list;
            }
            while ((line = br.readLine()) != null) {
                // Split the CSV line into parts, ensuring empty strings are captured for missing values
                String[] parts = line.split(",", -1);
                // Ensure there are enough parts to avoid ArrayIndexOutOfBoundsException
                if (parts.length < 7) {
                    System.err.println("Skipping malformed line in CSV: " + line);
                    continue;
                }

                try {
                    int id = Integer.parseInt(parts[0]);
                    String title = parts[1];
                    String desc = parts[2];
                    LocalDate due = LocalDate.parse(parts[3]);
                    String priority = parts[4];
                    String status = parts[5];
                    LocalDateTime createdAt = LocalDateTime.parse(parts[6]);
                    // Handle empty completedAt field by setting it to null
                    LocalDateTime completedAt = parts.length > 7 && !parts[7].isEmpty() ? LocalDateTime.parse(parts[7]) : null;

                    list.add(new Task(id, title, desc, due, priority, status, createdAt, completedAt));
                } catch (NumberFormatException | DateTimeParseException e) {
                    System.err.println("Error parsing task data from line (data format issue): " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading tasks from file: " + e.getMessage());
        }
        return list;
    }

    // Method to save tasks to the CSV file
    static void saveTasks(List<Task> tasks) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            // Write the CSV header
            pw.println("ID,Title,Description,DueDate,Priority,Status,CreatedAt,CompletedAt");
            // Write each task to a new line in CSV format
            for (Task t : tasks) {
                pw.println(t.toCSV());
            }
        } catch (IOException e) {
            System.out.println("Error saving tasks to file: " + e.getMessage());
        }
    }

    // Method to update the status of overdue tasks
    static void updateOverdueTasks(List<Task> tasks) {
        LocalDate today = LocalDate.now();
        for (Task t : tasks) {
            // If a task is pending and its due date is before today, mark it as overdue
            // unless it's already completed
            if (t.status.equals("Pending") && t.dueDate.isBefore(today)) {
                t.status = "Overdue";
            }
        }
    }
}
