package jenkins.plugins.nodejs.tools;

import static org.mockito.Mockito.*;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import hudson.model.Node;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CPU.class)
public class ToolsUtilsTest {

    @Before
    public void setup() {
        CPU[] cpuValues = CPU.values();
        CPU mock = PowerMockito.mock(CPU.class);
        for (CPU c : cpuValues) {
            Whitebox.setInternalState(mock, "name", c.name());
            Whitebox.setInternalState(mock, "ordinal", c.ordinal());
        }

        PowerMockito.mockStatic(CPU.class);
        PowerMockito.when(CPU.values()).thenReturn(cpuValues);
    }

    @Test
    public void nodejs_supports_32bit_64bit_on_windows_linux_mac() throws Exception {
        Node currentNode = mock(Node.class);
        
        when(CPU.of(currentNode)).thenReturn(CPU.amd64);
        CPU cpu = ToolsUtils.getCPU(currentNode, true);
        Assert.assertThat(cpu, CoreMatchers.is(CPU.i386));

        cpu = ToolsUtils.getCPU(currentNode);
        Assert.assertThat(cpu, CoreMatchers.is(CPU.amd64));
    }

    @Test(expected = DetectionFailedException.class)
    public void nodejs_doesn_t_supports_32bit_on_armv64() throws Exception {
        Node currentNode = mock(Node.class);

        when(CPU.of(currentNode)).thenReturn(CPU.arm64);
        ToolsUtils.getCPU(currentNode, true);
    }

    @Test
    public void nodejs_supports_32bit_on_armv6_armv7() throws Exception {
        Node currentNode = mock(Node.class);

        when(CPU.of(currentNode)).thenReturn(CPU.armv7l);
        CPU cpu = ToolsUtils.getCPU(currentNode, true);
        Assert.assertThat(cpu, CoreMatchers.is(CPU.armv7l));

        when(CPU.of(currentNode)).thenReturn(CPU.armv6l);
        cpu = ToolsUtils.getCPU(currentNode, true);
        Assert.assertThat(cpu, CoreMatchers.is(CPU.armv6l));
    }

}