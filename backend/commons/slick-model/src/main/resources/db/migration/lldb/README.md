# Long Lasting Data migrations

Folder containing migrations for Tower Long Lasting Data database.

## Naming guidelines

Name consists of:
- Date-time formatted numeric version
- Task number
- Description

Name regexp:
```
V{<curret-date-time>}__TS-\d+_[A-Za-z0-9_]+.sql
```

Where ```<curret-date-time>``` is timestamp of file creation formatted by java date
format specification (date time with no spaces):
```
yyyyMMddHHmmss
```


Examples:
```
V20180919111801__database_schema.sql
V20181002153600__TS-117_network_segments.sql
```

Seconds could be omitted by simply putting 00. Seconds could be used to resolve conflicts
in version numbers.

Collisions in versions needs to be resolved by programmer which wants to merge evolutions to
master. See Jenkins test result to verify that evolutions are correct.
