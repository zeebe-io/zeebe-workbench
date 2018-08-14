import org.scalatra._
import javax.servlet.ServletContext
import io.zeebe.workbench.webapp.TestRunnerServlet

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {

    // Mount our servlets as normal:
    context.mount(new TestRunnerServlet, "/*")
  }
}
