package jenkins.plugins.nodejs.tools;

/**
 * @author fcamblor
 */
public class NodeJSVersion implements Comparable<NodeJSVersion> {
    private Integer major;
    private Integer minor;
    private Integer patch;

    public NodeJSVersion(String version){
        String[] chunkedVersions = version.split("\\.");
        this.major = Integer.valueOf(chunkedVersions[0]);
        this.minor = Integer.valueOf(chunkedVersions[1]);
        this.patch = Integer.valueOf(chunkedVersions[2]);
    }

    public int compareTo(NodeJSVersion v) {
        int cmp = major.compareTo(v.major);
         if(cmp == 0){
           cmp = minor.compareTo(v.minor);
           if(cmp == 0){
             return patch.compareTo(v.patch);
           }
         }
         return cmp;
    }

    public boolean isLowerThan(NodeJSVersion version){
        return compareTo(version) < 0;
    }

    public boolean isLowerThan(String version){
        return isLowerThan(new NodeJSVersion(version));
    }

    public static int compare(String first, String second){
        return new NodeJSVersion(first).compareTo(new NodeJSVersion(second));
    }
}
