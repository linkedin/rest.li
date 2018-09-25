You can use the latest released version of Rest.li or build your own
local version.

## Using Rest.li JARs

If you are not modifying the Rest.li source code, you don’t need to
build Rest.li. You can simply depend on the [artifacts in the maven
central
repository](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.linkedin.pegasus%22)

The current latest version in Maven Central is 2.2.5.

For details on how to use Rest.li from its maven central artifacts, see:
[Quickstart](Quickstart:-A-Tutorial-Introduction-to-Rest.li)

## Building Your Own Copy of Rest.li

You can also checkout, modify, or build your own copy of Rest.li.

### Checking Out Source

You can get your own copy of the Rest.li repository with:

    <code>git clone git@github.com:linkedin/rest.li.wiki</code>

Or if you already have a copy of the repository, you can update it with:

    <code>git pull</code>

### Building Rest.li

To do a clean build type, do this:

    <code>gradle clean build</code>

To install the gradle JARs in your own local repository, do this:

    <code>gradle install</code>

#### Out of Memory While Trying to Build?

If the build fails with an error message saying that there isn’t enough
memory, increase the memory using the following command and try again:

\`\`\`  
export GRADLE\_OPTS=“-Xms512M -Xmx1536M -Xss1M
-XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=1024M”  
\`\`\`
