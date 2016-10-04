import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path

class Node {
    private String mName
    private def project

    void name(String name) {
        mName = name
    }

    Node(def project) {
        this.project = project
    }

    void build(File baseDir) {
        def cordaJAR = verifyAndGetCordaJar()
        File nodeDir = new File(baseDir, mName)
        createDirectories(nodeDir)
        copyCordaJAR(nodeDir, cordaJAR)
    }

    private void copyCordaJAR(File nodeDir, File cordaJar) {
        project.copy {
            from cordaJar
            into nodeDir
            rename cordaJar.name, 'corda.jar'
        }
    }

    private File verifyAndGetCordaJar() {
        def maybeCordaJAR = project.configurations.runtime.filter { it.toString().contains("corda-${project.corda_version}.jar")}
        if(maybeCordaJAR.size() == 0) {
            throw new RuntimeException("No Corda Capsule JAR found. Have you deployed the Corda project to Maven?")
        } else {
            def cordaJAR = maybeCordaJAR.getSingleFile()
            assert(cordaJAR.isFile())
            return cordaJAR
        }
    }

    private static void createDirectories(File nodeDir) {
        new File(nodeDir, "plugins").mkdirs()
        new File(nodeDir, "dependencies").mkdirs()
    }
}

class Cordform extends DefaultTask {
    private Path directory = Paths.get("./build/nodes")
    private List<Node> nodes = new ArrayList<Node>()

    public String directory(String directory) {
        this.directory = Paths.get(directory)
    }

    public void node(Closure configureClosure) {
        nodes << project.configure(new Node(project), configureClosure)
    }

    @TaskAction
    def build() {
        def dir = directory
        nodes.each {
            it.build(dir.toFile())
        }
    }
}

class Cordformation implements Plugin<Project> {
    void apply(Project project) {
        project.task("cordform", type: Cordform, dependsOn: ['build'])
    }
}
