package org.jenkinsci.plugins.zanata.zanatareposync;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

/**
 * @author Patrick Huang
 *         <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class TestUtils {
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Read a file from classpath and return its content as String.
     *
     * @param file
     *            relative file path to a text file
     * @return content of the file
     */
    static String readFileAsString(String file) {
        URL resource = Thread.currentThread().getContextClassLoader()
                .getResource(file);
        if (resource == null) {
            log.error("can not find {}", file);
            throw new IllegalStateException("can not find " + file);
        }
        try {
            List<String> lines = Files.readAllLines(
                    Paths.get(resource.getPath()), Charsets.UTF_8);
            String content = Joiner.on("\n").skipNulls().join(lines);
            log.debug("read {} with content: {}", file, content);
            return content;
        } catch (IOException e) {
            throw new RuntimeException("failed reading file:" + file);
        }
    }
}
