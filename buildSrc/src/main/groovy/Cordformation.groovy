import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class Node {
    private String mName

    void name(String name) {
        mName = name
    }
}

class Cordform extends DefaultTask {
    List<Node> nodes = new ArrayList<Node>()

    //public String prop(String test) {
    //    // Do Stuff
    //}
    
    public void node(Closure configureClosure) {
        project.configure(new Node(), configureClosure);
    }

    @TaskAction
    def build() {
        // Build nodes here
    }
}

class Cordformation implements Plugin<Project> {
    void apply(Project project) {
        project.task("cordform", type: Cordform, dependsOn: ['build'])
    }
}
