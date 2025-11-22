package modules

import com.google.inject.AbstractModule
import services.TaskNotificationPublisher


class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[TaskNotificationPublisher]).asEagerSingleton()
  }
}
