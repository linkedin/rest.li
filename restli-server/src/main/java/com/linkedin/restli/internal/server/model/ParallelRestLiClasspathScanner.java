package com.linkedin.restli.internal.server.model;

import com.linkedin.restli.server.ResourceConfigException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;


public class ParallelRestLiClasspathScanner extends RestLiClasspathScanner {
  private final ClassLoader _classLoader;
  private final Set<String> _packagePaths;

  public ParallelRestLiClasspathScanner(final Set<String> packageNames, final Set<String> classNames, final ClassLoader classLoader) {
    super(packageNames, classNames, classLoader);
    _classLoader = classLoader;
    _packagePaths = new HashSet<>();
    for (String packageName : packageNames) {
      _packagePaths.add(nameToPath(packageName));
    }

  }

  private void scanJar(final URI u) throws IOException {
    super.jarScannerAdapter(u);
  }

  private void scanDirectory(final File root) throws IOException {
    super.dirScannerAdapter(root);
  }

  private String nameToPath(final String name) {
    return name.replace(PACKAGE_SEPARATOR, FILE_SEPARATOR);
  }

  private String toUnixPath(final String path) {
    return path.replace(FILE_SEPARATOR, UNIX_FILE_SEPARATOR);
  }

  @Override
  public void scanPackages() {
    try {
      ForkJoinPool pool = new ForkJoinPool(16);
      List<URL> resourceList = new ArrayList<>();
      for (String p : _packagePaths) {
        resourceList.addAll(Collections.list(_classLoader.getResources(toUnixPath(p))));
      }

      pool.invoke(new ScanJarOrDirTask(resourceList));
      pool.shutdown();
    } catch (IOException e) {
      throw new ResourceConfigException("Unable to scan resources", e);
    }
  }

  private class ScanJarOrDirTask extends RecursiveAction {
    private static final long serialVersionUID = 10002;
    private List<URL> _list;

    private ScanJarOrDirTask(List<URL> list) {
      _list = list;
    }

    @Override
    protected void compute() {
      if (_list.size() <= 4) {
        try {
          for (URL url : _list) {
            URI u = url.toURI();
            String scheme = u.getScheme();
            switch (scheme) {
              case SCHEME_JAR:
              case SCHEME_ZIP:
                scanJar(u);
                break;
              case SCHEME_FILE:
                scanDirectory(new File(u.getPath()));
                break;
              default:
                throw new ResourceConfigException(
                    "Unable to scan resource '" + u.toString() + "'. URI scheme not supported by scanner.");
            }
          }

        } catch (IOException | URISyntaxException e) {
          throw new ResourceConfigException("Unable to scan resource", e);
        }

      } else {
        int delimt = _list.size() / 4;
        List<URL> sublist1 = _list.subList(0, delimt);
        List<URL> sublist2 = _list.subList(delimt, delimt * 2);
        List<URL> sublist3 = _list.subList(delimt * 2, delimt * 3);
        List<URL> sublist4 = _list.subList(delimt * 3, _list.size());
        ScanJarOrDirTask subTask1 = new ScanJarOrDirTask(sublist1);
        ScanJarOrDirTask subTask2 = new ScanJarOrDirTask(sublist2);
        ScanJarOrDirTask subTask3 = new ScanJarOrDirTask(sublist3);
        ScanJarOrDirTask subTask4 = new ScanJarOrDirTask(sublist4);
        invokeAll(subTask1, subTask2, subTask3, subTask4);
      }

    }

  }

}
