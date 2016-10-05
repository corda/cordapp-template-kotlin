class Node {
    public String name
    private String dirName
    private String nearestCity
    private Boolean isNotary = false
    private Boolean isHttps = false
    private List<String> advertisedServices = []
    private Integer artemisPort
    private Integer webPort
    private String networkMapAddress = ""

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

    void networkMapAddress(String networkMapAddress) {
        this.networkMapAddress = networkMapAddress
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

    String getArtemisAddress() {
        return "localhost:" + artemisPort
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
                        .replaceAll('@@networkMapAddress', networkMapAddress)
                        .replaceAll('@@artemisPort', artemisPort)
                        .replaceAll('@@webPort@@', webPort)
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
