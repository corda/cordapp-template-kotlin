import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class TestHandler {

}

class Cordform extends DefaultTask {
    @Input
    def test = new TestHandler()

    @TaskAction
    def greet() {
        println test
    }
}

class Cordformation implements Plugin<Project> {
    void apply(Project project) {
        project.task("cordform", type: Cordform, dependsOn: ['build'])
    }
}
