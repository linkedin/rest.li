# Testing IntelliJ Plugins

The IntelliJ Platform SDK provides extensive plugin testing support through the
JUnit test framework. All LinkedIn IntelliJ plugins must have unit tests.

### Test Directory structure

```
├── test/
│   ├── README.md
│   ├── java
│   │   ├── Unit tests code
│   ├── resources
│   │   ├── resources files (input, output, etc. )
```

Once you add you own tests and resources fill free to delete .gitkeep files.

### Notes & Tips

- If running these unit tests directly in IDEA, ensure that your test configurations all use `$MODULE_WORKING_DIR$` as
  the working directory. For some reason, though, the tests in {@link PdlFileTemplateTest} won't work in IDEA.
- *WARNING:* The result of `PdlFullnameStubIndex#getAllKeys(Project)` inside a unit test may return all `fullname` keys
  in the project even if only certain types are exposed to the test fixture. A subsequent call to
  `PdlFullnameIndex#get(String, Project, GlobalSearchScope)` may result in an empty result set even if using a
  `fullname` retrieved from this index.
    - It's not clear why this is, but it may be worthwhile to investigate this if given the time.
    - This affects the logic in `FullyQualifiedNameCompletionProvider`.

### References

Refer to the [Testing Plugins](http://www.jetbrains.org/intellij/sdk/docs/basics/testing_plugins.html)
section of the [IntelliJ Platform SDK](http://www.jetbrains.org/intellij/sdk/docs/)
for information about testing IntelliJ plugins.

For a few samples please checkout sample-intellij-plugin multiproduct.