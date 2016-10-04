import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths
import java.nio.file.Path

class Node {
    private String name
    private String dirName
    private String nearestCity
    private Boolean isNotary = false
    private Boolean isHttps = false
    private List<String> advertisedServices = []
    private Integer artemisPort
    private Integer webPort
    public String networkMapAddress

    private File nodeDir
    private def project

    void name(String name) {
        this.name = name
    }

    void dirName(String dirName) {
        this.dirName = dirName
    }

    void nearestCity(String nearestCity) {
        this.nearestCity = nearestCity
    }

    void notary(Boolean isNotary) {
        this.isNotary = isNotary
    }

    void https(Boolean isHttps) {
        this.isHttps = isHttps
    }

    void advertisedServices(List<String> advertisedServices) {
        this.advertisedServices = advertisedServices
    }

    void artemisPort(Integer artemisPort) {
        this.artemisPort = artemisPort
    }

    void webPort(Integer webPort) {
        this.webPort = webPort
    }

    Node(def project) {
        this.project = project
    }

    void build(File baseDir) {
        nodeDir = new File(baseDir, dirName)
        installCordaJAR()
        installPlugins()
        installDependencies()
        installConfig()
    }

    private void installCordaJAR() {
        def cordaJar = verifyAndGetCordaJar()
        project.copy {
            from cordaJar
            into nodeDir
            rename cordaJar.name, 'corda.jar'
        }
    }

    private void installPlugins() {
        def cordaJar = verifyAndGetCordaJar()
        def pluginsDir = getAndCreateDirectory(nodeDir, "plugins")
        def appDeps = project.configurations.runtime.filter { it != cordaJar } // TODO: Filter out all other deps in the corda jar
        project.copy {
            from appDeps
            into pluginsDir
        }
    }

    private void installDependencies() {
        // TODO:
    }

    private void installConfig() {
        project.copy {
            from ('./config/dev/nodetemplate.conf') {
                filter { it
                    .replaceAll('@@name@@', name)
                    .replaceAll('@@dirName@@', dirName)
                    .replaceAll('@@nearestCity@@', nearestCity)
                    .replaceAll('@@isNotary@@', isNotary.toString())
                    .replaceAll('@@isHttps@@', isHttps.toString())
                    .replaceAll('@@advertisedServices@@', isHttps.toString())
                }
            }
            into nodeDir
            rename 'nodetemplate.conf', 'node.conf'
        }
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
    private String networkMapNodeName

    public String directory(String directory) {
        this.directory = Paths.get(directory)
    }

    public String networkMap(String nodeName) {
        networkMapNodeName = nodeName
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
