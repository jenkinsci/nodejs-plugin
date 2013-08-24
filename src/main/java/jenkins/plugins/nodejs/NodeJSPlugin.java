package jenkins.plugins.nodejs;

import hudson.Plugin;
import hudson.model.Hudson;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;

import java.io.IOException;

/**
 * @author fcamblor
 */
public class NodeJSPlugin extends Plugin {

    NodeJSInstallation[] installations;

    public NodeJSPlugin(){
        super();
    }

    @Override
   	public void start() throws Exception {
   		super.start();

   		this.load();

   		// If installations have not been read in nodejs.xml, let's initialize them
   		if(this.installations == null){
            this.installations = new NodeJSInstallation[0];
   		}
   	}

    public NodeJSInstallation[] getInstallations() {
        return installations;
    }

    public NodeJSInstallation findInstallationByName(String name) {
        for(NodeJSInstallation nodeJSInstallation : getInstallations()){
            if(name.equals(nodeJSInstallation.getName())){
                return nodeJSInstallation;
            }
        }
        throw new IllegalArgumentException("NodeJS Installation not found : "+name);
    }

    public void setInstallations(NodeJSInstallation[] installations) {
        this.installations = installations;
        try {
            this.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static NodeJSPlugin instance() {
        return Hudson.getInstance().getPlugin(NodeJSPlugin.class);
    }
}
