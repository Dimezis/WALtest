# WALtest 
When performing a query on `SQLiteDatabase`, the `SQLiteProgram` from `android.database.sqlite` performs `SQLiteStatementType.getSqlStatementType(mSql)` check to guess the query type and to prepare a SQL statement for execution.
See - https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/database/sqlite/SQLiteProgram.java;l=60-64;drc=ee976fd24cfbd567c4543da39aadb1b3e4567042

This query type guessing is very basic and naive, relying on the first 3 letters of the SQL statement - https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/database/DatabaseUtils.java;drc=ee976fd24cfbd567c4543da39aadb1b3e4567042;l=1566

So for example, if the query is a Select query, but contains a comment like this:
```
   -- comment
   select * from employees limit 1;
```
It will NOT be recognized as a Select query and `assumeReadOnly` will be false.

Most of the time it doesn't matter, but with WAL mode enabled, we expect the SQLite to be able to perform read queries during writing, but this `assumeReadOnly` breaks this behavior.

See the sample code that demonstrates the issue
https://github.com/Dimezis/WALtest/blob/master/app/src/main/java/com/example/waltest/MainActivity.kt. 
Pay attention to `fastReadQuery` method and its comment.

This app performs basic insert transactions on 1 writer thread, and reads with `select` on the other thread with DB in WAL mode.
If you remove the comment `-- comment` from the query, you can see that the reading is done concurrently according to logs and their timestamps:
- WAL write start
- WAL read start
- WAL read end
- WAL write end

If you leave the `-- comment` though, concurrent reading doesn't work anymore and the output is:

- WAL write start
- WAL read start
- WAL write end
- WAL read end



