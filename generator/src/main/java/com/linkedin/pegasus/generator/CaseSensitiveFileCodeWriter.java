package com.linkedin.pegasus.generator;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Similar to {@link com.sun.codemodel.FileCodeWriter} but has ability to create directories in lower case.
 */
public class CaseSensitiveFileCodeWriter extends CodeWriter {
    /** The target directory to put source code. */
    private final File target;

    /** specify whether or not to mark the generated files read-only. */
    private final boolean readOnly;

    /** Files that shall be marked as read only. */
    private final Set<File> readonlyFiles = new HashSet<File>();

    /** True, generated directories to be created in lower case; False, otherwise. */
    private boolean generateLowercasePath;

    public CaseSensitiveFileCodeWriter(File target, boolean readOnly, boolean generateLowercasePath) throws IOException
    {
        this.target = target;
        this.readOnly = readOnly;
        if (!target.exists() || !target.isDirectory()) {
            throw new IOException(target + ": non-existent directory");
        }
        this.generateLowercasePath = generateLowercasePath;
    }

    public OutputStream openBinary(JPackage pkg, String fileName) throws IOException
    {
        return new FileOutputStream(getFile(pkg, fileName));
    }

    protected File getFile(JPackage pkg, String fileName) throws IOException
    {
        File dir;
        if (pkg.isUnnamed()) {
            dir = target;
        } else {
            dir = new File(target, toDirName(pkg));
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }

        File fn = new File(dir, fileName);

        if (fn.exists()) {
            if (!fn.delete())
                throw new IOException(fn + ": Can't delete previous version");
        }

        if (readOnly) {
            readonlyFiles.add(fn);
        }
        return fn;
    }

    public void close() throws IOException
    {
        // mark files as read-only if necessary
        for (File f : readonlyFiles) {
            f.setReadOnly();
        }
    }

    /** Converts a package name to the directory name. */
    private String toDirName(JPackage pkg)
    {
        String packageName = generateLowercasePath ? pkg.name().toLowerCase() : pkg.name();
        return packageName.replace('.', File.separatorChar);
    }
}
