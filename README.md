# Task Tracker CLI

A simple command-line tool to track tasks, built with plain Python (no external dependencies). Tasks are stored in `tasks.json` in the current directory.

Project idea: https://roadmap.sh/projects/task-tracker

## Requirements

- Python 3.7+

## Usage

Run via `python3 task_cli.py <command> [args]`. Optionally make it executable and alias it as `task-cli`:

```bash
chmod +x task_cli.py
alias task-cli="python3 $(pwd)/task_cli.py"
```

### Commands

```bash
# Add a new task
task-cli add "Buy groceries"
# Output: Task added successfully (ID: 1)

# Update a task
task-cli update 1 "Buy groceries and cook dinner"

# Delete a task
task-cli delete 1

# Mark a task as in-progress or done
task-cli mark-in-progress 1
task-cli mark-done 1

# List all tasks
task-cli list

# List tasks by status
task-cli list done
task-cli list todo
task-cli list in-progress
```

## Task properties

Each task stored in `tasks.json` has:

- `id`: unique identifier
- `description`: short description
- `status`: `todo`, `in-progress`, or `done`
- `createdAt`: ISO 8601 timestamp
- `updatedAt`: ISO 8601 timestamp

`tasks.json` is created automatically on the first `add` if it doesn't exist.
