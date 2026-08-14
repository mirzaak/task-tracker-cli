#!/usr/bin/env python3
"""Task Tracker CLI - manage tasks from the command line, stored in tasks.json."""

import json
import os
import sys
from datetime import datetime, timezone

TASKS_FILE = os.path.join(os.getcwd(), "tasks.json")
VALID_STATUSES = ("todo", "in-progress", "done")


def load_tasks():
    if not os.path.exists(TASKS_FILE):
        return []
    try:
        with open(TASKS_FILE, "r") as f:
            content = f.read().strip()
            if not content:
                return []
            return json.loads(content)
    except json.JSONDecodeError:
        print(f"Error: {TASKS_FILE} is corrupted or not valid JSON.")
        sys.exit(1)
    except OSError as e:
        print(f"Error reading {TASKS_FILE}: {e}")
        sys.exit(1)


def save_tasks(tasks):
    try:
        with open(TASKS_FILE, "w") as f:
            json.dump(tasks, f, indent=2)
    except OSError as e:
        print(f"Error writing {TASKS_FILE}: {e}")
        sys.exit(1)


def now_iso():
    return datetime.now(timezone.utc).isoformat()


def next_id(tasks):
    if not tasks:
        return 1
    return max(t["id"] for t in tasks) + 1


def find_task(tasks, task_id):
    for t in tasks:
        if t["id"] == task_id:
            return t
    return None


def parse_id(raw_id):
    try:
        return int(raw_id)
    except ValueError:
        print(f"Error: '{raw_id}' is not a valid task ID.")
        sys.exit(1)


def cmd_add(args):
    if len(args) < 1:
        print("Usage: task-cli add <description>")
        sys.exit(1)
    description = args[0]
    tasks = load_tasks()
    task = {
        "id": next_id(tasks),
        "description": description,
        "status": "todo",
        "createdAt": now_iso(),
        "updatedAt": now_iso(),
    }
    tasks.append(task)
    save_tasks(tasks)
    print(f"Task added successfully (ID: {task['id']})")


def cmd_update(args):
    if len(args) < 2:
        print("Usage: task-cli update <id> <description>")
        sys.exit(1)
    task_id = parse_id(args[0])
    description = args[1]
    tasks = load_tasks()
    task = find_task(tasks, task_id)
    if task is None:
        print(f"Error: Task with ID {task_id} not found.")
        sys.exit(1)
    task["description"] = description
    task["updatedAt"] = now_iso()
    save_tasks(tasks)
    print(f"Task {task_id} updated successfully")


def cmd_delete(args):
    if len(args) < 1:
        print("Usage: task-cli delete <id>")
        sys.exit(1)
    task_id = parse_id(args[0])
    tasks = load_tasks()
    task = find_task(tasks, task_id)
    if task is None:
        print(f"Error: Task with ID {task_id} not found.")
        sys.exit(1)
    tasks = [t for t in tasks if t["id"] != task_id]
    save_tasks(tasks)
    print(f"Task {task_id} deleted successfully")


def cmd_mark(args, status):
    if len(args) < 1:
        print(f"Usage: task-cli mark-{status} <id>")
        sys.exit(1)
    task_id = parse_id(args[0])
    tasks = load_tasks()
    task = find_task(tasks, task_id)
    if task is None:
        print(f"Error: Task with ID {task_id} not found.")
        sys.exit(1)
    task["status"] = status
    task["updatedAt"] = now_iso()
    save_tasks(tasks)
    print(f"Task {task_id} marked as {status}")


def print_tasks(tasks):
    if not tasks:
        print("No tasks found.")
        return
    for t in tasks:
        print(f"[{t['id']}] ({t['status']}) {t['description']}  "
              f"created: {t['createdAt']}  updated: {t['updatedAt']}")


def cmd_list(args):
    tasks = load_tasks()
    if not args:
        print_tasks(tasks)
        return
    status_filter = args[0]
    if status_filter not in VALID_STATUSES:
        print(f"Error: invalid status filter '{status_filter}'. "
              f"Use one of: {', '.join(VALID_STATUSES)}")
        sys.exit(1)
    filtered = [t for t in tasks if t["status"] == status_filter]
    print_tasks(filtered)


def print_usage():
    print("""Task Tracker CLI

Usage:
  task-cli add <description>
  task-cli update <id> <description>
  task-cli delete <id>
  task-cli mark-in-progress <id>
  task-cli mark-done <id>
  task-cli list
  task-cli list done
  task-cli list todo
  task-cli list in-progress""")


def main():
    argv = sys.argv[1:]
    if not argv:
        print_usage()
        sys.exit(1)

    command, rest = argv[0], argv[1:]

    if command == "add":
        cmd_add(rest)
    elif command == "update":
        cmd_update(rest)
    elif command == "delete":
        cmd_delete(rest)
    elif command == "mark-in-progress":
        cmd_mark(rest, "in-progress")
    elif command == "mark-done":
        cmd_mark(rest, "done")
    elif command == "list":
        cmd_list(rest)
    elif command in ("-h", "--help", "help"):
        print_usage()
    else:
        print(f"Error: unknown command '{command}'")
        print_usage()
        sys.exit(1)


if __name__ == "__main__":
    main()
