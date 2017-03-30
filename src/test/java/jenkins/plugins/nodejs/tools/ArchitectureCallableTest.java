package jenkins.plugins.nodejs.tools;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.File;

import org.junit.Test;

public class ArchitectureCallableTest {

    @Test
    public void test_uname_on_linux() throws Exception {
        assumeThat(Platform.current(), isOneOf(Platform.LINUX, Platform.OSX));

        CPU.ArchitectureCallable callable = new CPU.ArchitectureCallable();
        String machine = callable.invoke(new File("/"), null);
        assertThat(machine, anyOf(containsString("86"), containsString("64"), containsString("arm")));
    }

}