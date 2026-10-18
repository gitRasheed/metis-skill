Two changes for the habit tracker for the next release:

1. Rename the `due` field to `due_date` everywhere. People open the JSON file by hand and `due` confuses them.
2. Habits can carry a list of string tags. `habits add stretch --tag health --tag morning` sets them, and `habits list --tag health` shows only habits with that tag.
