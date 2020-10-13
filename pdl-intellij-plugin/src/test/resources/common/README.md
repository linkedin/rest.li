`external-pegasus.jar`:

```
  Length      Date    Time    Name
---------  ---------- -----   ----
        0  01-23-2020 19:49   META-INF/
       69  01-23-2020 19:49   META-INF/MANIFEST.MF
        0  01-23-2020 19:43   ignoreMe/
        0  01-23-2020 19:43   ignoreMe/com/
        0  01-23-2020 19:43   ignoreMe/com/ignore/
      102  01-23-2020 19:43   ignoreMe/com/ignore/ShouldBeIgnored.pdsc
        0  03-08-2017 10:34   pegasus/
        0  03-08-2017 10:34   pegasus/org/
        0  03-08-2017 10:35   pegasus/org/example/
       94  03-08-2017 10:35   pegasus/org/example/ExternalPdsc.pdsc
       61  03-08-2017 10:34   pegasus/org/example/ExternalPdl.pdl
---------                     -------
      326                     11 files
```

*Note:* `ignoreMe/` is included to ensure that only schemas under `pegasus/` are detected. This is implicitly tested
by the completion unit testing.