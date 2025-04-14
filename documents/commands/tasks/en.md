# `/tasks`: Manage tasks of the server

The [command](../en.md) is used to manage the scheduled tasks of the server.

When you modify masses of blocks with commands such as `/setblocks`, `/draw` while not setting `immediately=true`, a scheduled task will be created, to split a task into multiple ticks, avoiding server lags caused by completing masses of tasks in one single tick.

## Syntax

- `/tasks`: Identical to `/tasks list`.
- `/tasks count`: Return the number of current scheduled tasks.
- `/tasks clear`: Clear all scheduled tasks, even if the tasks are not completed yet.
- `/tasks remove <uuid>`: Remove the scheduled task, even if the task is not completed yet.
- `/tasks suspend <uuid>`: Suspend a scheduled task. Execution fails for tasks already suspended.
- `/tasks continue <uuid>`: Continue a scheduled task that has been already suspended. Execution fails for tasks not suspended.
- `/tasks sprint <uuid> [progress number]`: Sprint to complete the scheduled task, or complete the specified number of steps, which means even if amounts of progress is not completed, it will be completed in one single tick, which may cause the server lag for a while.
- `/tasks list [limit]`: List the current scheduled tasks, including the tasks' name, state and provide buttons of operations.

## Parameters

- `<uuid>`: The UUID of the tasks. Execution fails if the task does not exist. You can get all current tasks with `/tasks list`, and operate for a certain task.
- `[progress number]`: The number of steps of tasks to sprint. If not specified, the whole task will be sprinted to complete.
- `[limit]`: Limit the number of tasks shown in the current page.