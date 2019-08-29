To run the JMH benchmarks for the `data` module:

ligradle -Dorg.gradle.caching=false --no-daemon :data:jmh

The runner is flaky with the Gradle daemon and JMH is flaky with distributed caching (especially when you are making
changes to the benchmarks) so we disable both.
