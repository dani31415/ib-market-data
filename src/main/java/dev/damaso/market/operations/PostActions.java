package dev.damaso.market.operations;

public class PostActions {
    private Jenkins jenkins;

    public String notifyJenkins = null;

    public void setJenkins(Jenkins jenkins) {
        this.jenkins = jenkins;
    }

    public void postActions() {
        if (notifyJenkins != null) {
            this.jenkins.notifyJenkinsAction(notifyJenkins);
        }
    }
}
