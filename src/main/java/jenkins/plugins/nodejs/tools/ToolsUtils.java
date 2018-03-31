package jenkins.plugins.nodejs.tools;

import hudson.model.Node;
import jenkins.plugins.nodejs.Messages;

/*package */ class ToolsUtils {

    private ToolsUtils() {
    }

    public static Platform getPlatform(Node node) throws DetectionFailedException {
        return Platform.of(node);
    }

    public static CPU getCPU(Node node) throws DetectionFailedException {
        return getCPU(node, false);
    }

    public static CPU getCPU(Node node, boolean force32bit) throws DetectionFailedException {
        CPU nodeCPU = CPU.of(node);
        if (force32bit) {
            if (!support32Bit(nodeCPU)) {
                throw new DetectionFailedException(Messages.SystemTools_unsupported32bitArchitecture());
            }

            // force 32 bit architecture 
            if (nodeCPU == CPU.amd64) {
                nodeCPU = CPU.i386;
            }
        }
        return nodeCPU;
    }

    private static boolean support32Bit(CPU cpu) {
        switch (cpu) {
        case armv6l:
            // 64bit start with ARMv8
        case armv7l:
        case i386:
        case amd64:
            return true;
        default:
            return false;
        }
    }

}