import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Task Tracker CLI - manage tasks from the command line, stored in tasks.json.
 * No external libraries: file I/O via java.nio, JSON hand-rolled.
 */
public class TaskCli {

    private static final Path TASKS_FILE = Paths.get(System.getProperty("user.dir"), "tasks.json");
    private static final List<String> VALID_STATUSES = List.of("todo", "in-progress", "done");

    static class Task {
        int id;
        String description;
        String status;
        String createdAt;
        String updatedAt;

        Task(int id, String description, String status, String createdAt, String updatedAt) {
            this.id = id;
            this.description = description;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);

        switch (command) {
            case "add" -> cmdAdd(rest);
            case "update" -> cmdUpdate(rest);
            case "delete" -> cmdDelete(rest);
            case "mark-in-progress" -> cmdMark(rest, "in-progress");
            case "mark-done" -> cmdMark(rest, "done");
            case "list" -> cmdList(rest);
            case "-h", "--help", "help" -> printUsage();
            default -> {
                System.out.println("Error: unknown command '" + command + "'");
                printUsage();
                System.exit(1);
            }
        }
    }

    // ---- commands ----

    private static void cmdAdd(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: task-cli add <description>");
            System.exit(1);
        }
        String description = args[0];
        List<Task> tasks = loadTasks();
        String now = nowIso();
        Task task = new Task(nextId(tasks), description, "todo", now, now);
        tasks.add(task);
        saveTasks(tasks);
        System.out.println("Task added successfully (ID: " + task.id + ")");
    }

    private static void cmdUpdate(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: task-cli update <id> <description>");
            System.exit(1);
        }
        int id = parseId(args[0]);
        String description = args[1];
        List<Task> tasks = loadTasks();
        Task task = findTask(tasks, id);
        if (task == null) {
            System.out.println("Error: Task with ID " + id + " not found.");
            System.exit(1);
        }
        task.description = description;
        task.updatedAt = nowIso();
        saveTasks(tasks);
        System.out.println("Task " + id + " updated successfully");
    }

    private static void cmdDelete(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: task-cli delete <id>");
            System.exit(1);
        }
        int id = parseId(args[0]);
        List<Task> tasks = loadTasks();
        Task task = findTask(tasks, id);
        if (task == null) {
            System.out.println("Error: Task with ID " + id + " not found.");
            System.exit(1);
        }
        tasks.remove(task);
        saveTasks(tasks);
        System.out.println("Task " + id + " deleted successfully");
    }

    private static void cmdMark(String[] args, String status) {
        if (args.length < 1) {
            System.out.println("Usage: task-cli mark-" + status + " <id>");
            System.exit(1);
        }
        int id = parseId(args[0]);
        List<Task> tasks = loadTasks();
        Task task = findTask(tasks, id);
        if (task == null) {
            System.out.println("Error: Task with ID " + id + " not found.");
            System.exit(1);
        }
        task.status = status;
        task.updatedAt = nowIso();
        saveTasks(tasks);
        System.out.println("Task " + id + " marked as " + status);
    }

    private static void cmdList(String[] args) {
        List<Task> tasks = loadTasks();
        if (args.length == 0) {
            printTasks(tasks);
            return;
        }
        String statusFilter = args[0];
        if (!VALID_STATUSES.contains(statusFilter)) {
            System.out.println("Error: invalid status filter '" + statusFilter
                    + "'. Use one of: " + String.join(", ", VALID_STATUSES));
            System.exit(1);
        }
        List<Task> filtered = new ArrayList<>();
        for (Task t : tasks) {
            if (t.status.equals(statusFilter)) {
                filtered.add(t);
            }
        }
        printTasks(filtered);
    }

    private static void printTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }
        for (Task t : tasks) {
            System.out.println("[" + t.id + "] (" + t.status + ") " + t.description
                    + "  created: " + t.createdAt + "  updated: " + t.updatedAt);
        }
    }

    private static void printUsage() {
        System.out.println("""
                Task Tracker CLI

                Usage:
                  task-cli add <description>
                  task-cli update <id> <description>
                  task-cli delete <id>
                  task-cli mark-in-progress <id>
                  task-cli mark-done <id>
                  task-cli list
                  task-cli list done
                  task-cli list todo
                  task-cli list in-progress""");
    }

    // ---- helpers ----

    private static int parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            System.out.println("Error: '" + raw + "' is not a valid task ID.");
            System.exit(1);
            return -1; // unreachable
        }
    }

    private static Task findTask(List<Task> tasks, int id) {
        for (Task t : tasks) {
            if (t.id == id) {
                return t;
            }
        }
        return null;
    }

    private static int nextId(List<Task> tasks) {
        int max = 0;
        for (Task t : tasks) {
            if (t.id > max) {
                max = t.id;
            }
        }
        return max + 1;
    }

    private static String nowIso() {
        return Instant.now().toString();
    }

    // ---- persistence (hand-rolled JSON, no external libraries) ----

    private static List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();
        if (!Files.exists(TASKS_FILE)) {
            return tasks;
        }
        String content;
        try {
            content = Files.readString(TASKS_FILE, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            System.out.println("Error reading " + TASKS_FILE + ": " + e.getMessage());
            System.exit(1);
            return tasks;
        }
        if (content.isEmpty()) {
            return tasks;
        }
        try {
            return Json.parseTasks(content);
        } catch (RuntimeException e) {
            System.out.println("Error: " + TASKS_FILE + " is corrupted or not valid JSON.");
            System.exit(1);
            return tasks;
        }
    }

    private static void saveTasks(List<Task> tasks) {
        try {
            Files.writeString(TASKS_FILE, Json.toJson(tasks), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Error writing " + TASKS_FILE + ": " + e.getMessage());
            System.exit(1);
        }
    }

    // ---- minimal JSON support tailored to the Task schema ----

    private static class Json {

        static String toJson(List<Task> tasks) {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                sb.append("  {\n");
                sb.append("    \"id\": ").append(t.id).append(",\n");
                sb.append("    \"description\": ").append(quote(t.description)).append(",\n");
                sb.append("    \"status\": ").append(quote(t.status)).append(",\n");
                sb.append("    \"createdAt\": ").append(quote(t.createdAt)).append(",\n");
                sb.append("    \"updatedAt\": ").append(quote(t.updatedAt)).append("\n");
                sb.append("  }");
                if (i < tasks.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("]");
            return sb.toString();
        }

        private static String quote(String s) {
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                    }
                }
            }
            sb.append('"');
            return sb.toString();
        }

        // Tiny recursive-descent parser, just enough for an array of flat
        // string/int objects with the Task field names.
        static List<Task> parseTasks(String content) {
            Parser p = new Parser(content);
            p.skipWhitespace();
            List<Task> tasks = new ArrayList<>();
            p.expect('[');
            p.skipWhitespace();
            if (p.peek() == ']') {
                p.next();
                return tasks;
            }
            while (true) {
                p.skipWhitespace();
                tasks.add(parseTask(p));
                p.skipWhitespace();
                char c = p.next();
                if (c == ',') {
                    continue;
                } else if (c == ']') {
                    break;
                } else {
                    throw new RuntimeException("Expected ',' or ']' at position " + p.pos);
                }
            }
            return tasks;
        }

        private static Task parseTask(Parser p) {
            p.skipWhitespace();
            p.expect('{');
            Integer id = null;
            String description = null, status = null, createdAt = null, updatedAt = null;
            p.skipWhitespace();
            if (p.peek() == '}') {
                p.next();
                return new Task(0, "", "todo", "", "");
            }
            while (true) {
                p.skipWhitespace();
                String key = p.parseString();
                p.skipWhitespace();
                p.expect(':');
                p.skipWhitespace();
                switch (key) {
                    case "id" -> id = (int) p.parseNumber();
                    case "description" -> description = p.parseString();
                    case "status" -> status = p.parseString();
                    case "createdAt" -> createdAt = p.parseString();
                    case "updatedAt" -> updatedAt = p.parseString();
                    default -> p.skipValue();
                }
                p.skipWhitespace();
                char c = p.next();
                if (c == ',') {
                    continue;
                } else if (c == '}') {
                    break;
                } else {
                    throw new RuntimeException("Expected ',' or '}' at position " + p.pos);
                }
            }
            if (id == null || description == null || status == null
                    || createdAt == null || updatedAt == null) {
                throw new RuntimeException("Task missing required field");
            }
            return new Task(id, description, status, createdAt, updatedAt);
        }

        private static class Parser {
            final String s;
            int pos = 0;

            Parser(String s) {
                this.s = s;
            }

            char peek() {
                if (pos >= s.length()) {
                    throw new RuntimeException("Unexpected end of input");
                }
                return s.charAt(pos);
            }

            char next() {
                char c = peek();
                pos++;
                return c;
            }

            void expect(char c) {
                char actual = next();
                if (actual != c) {
                    throw new RuntimeException("Expected '" + c + "' but got '" + actual + "' at position " + (pos - 1));
                }
            }

            void skipWhitespace() {
                while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                    pos++;
                }
            }

            String parseString() {
                expect('"');
                StringBuilder sb = new StringBuilder();
                while (true) {
                    char c = next();
                    if (c == '"') {
                        break;
                    } else if (c == '\\') {
                        char esc = next();
                        switch (esc) {
                            case '"' -> sb.append('"');
                            case '\\' -> sb.append('\\');
                            case '/' -> sb.append('/');
                            case 'n' -> sb.append('\n');
                            case 'r' -> sb.append('\r');
                            case 't' -> sb.append('\t');
                            case 'u' -> {
                                String hex = s.substring(pos, pos + 4);
                                pos += 4;
                                sb.append((char) Integer.parseInt(hex, 16));
                            }
                            default -> throw new RuntimeException("Invalid escape '\\" + esc + "'");
                        }
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }

            double parseNumber() {
                int start = pos;
                while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
                    pos++;
                }
                return Double.parseDouble(s.substring(start, pos));
            }

            void skipValue() {
                skipWhitespace();
                char c = peek();
                if (c == '"') {
                    parseString();
                } else if (c == '{') {
                    next();
                    skipWhitespace();
                    if (peek() == '}') {
                        next();
                        return;
                    }
                    while (true) {
                        skipWhitespace();
                        parseString();
                        skipWhitespace();
                        expect(':');
                        skipWhitespace();
                        skipValue();
                        skipWhitespace();
                        char cc = next();
                        if (cc == '}') break;
                        if (cc != ',') throw new RuntimeException("Expected ',' or '}'");
                    }
                } else if (c == '[') {
                    next();
                    skipWhitespace();
                    if (peek() == ']') {
                        next();
                        return;
                    }
                    while (true) {
                        skipWhitespace();
                        skipValue();
                        skipWhitespace();
                        char cc = next();
                        if (cc == ']') break;
                        if (cc != ',') throw new RuntimeException("Expected ',' or ']'");
                    }
                } else if (c == 't') {
                    pos += 4; // true
                } else if (c == 'f') {
                    pos += 5; // false
                } else if (c == 'n') {
                    pos += 4; // null
                } else {
                    parseNumber();
                }
            }
        }
    }
}
