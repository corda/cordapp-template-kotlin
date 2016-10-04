import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

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
        File nodeDir = new File(baseDir, mName)
        installCordaJAR(nodeDir)
        installPlugins(nodeDir)
        installDependencies(nodeDir)
    }

    private void installCordaJAR(File nodeDir) {
        def cordaJar = verifyAndGetCordaJar()
        project.copy {
            from cordaJar
            into nodeDir
            rename cordaJar.name, 'corda.jar'
        }
    }

    private void installPlugins(File nodeDir) {
        def cordaJar = verifyAndGetCordaJar()
        def pluginsDir = getAndCreateDirectory(nodeDir, "plugins")
        def appDeps = project.configurations.runtime.filter { it != cordaJar } // TODO: Filter out all other deps in the corda jar
        project.copy {
            from appDeps
            into pluginsDir
        }
    }

    private void installDependencies(File nodeDir) {
        // TODO:
    }

    private File verifyAndGetCordaJar() {
        def maybeCordaJAR = project.configurations.runtime.filter { it.toString().contains("corda-${project.corda_version}.jar")}
        if(maybeCordaJAR.size() == 0) {
            throw new RuntimeException("No Corda Capsule JAR found. Have you deployed the Corda project to Maven?")
        } else {
            def cordaJar = maybeCordaJAR.getSingleFile()
            assert(cordaJar.isFile())
            return cordaJar
        }
    }

    private static File getAndCreateDirectory(File baseDir, String subDirName) {
        File dir = new File(baseDir, subDirName)
        assert(!dir.exists() || dir.isDirectory())
        dir.mkdirs()
        return dir
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
