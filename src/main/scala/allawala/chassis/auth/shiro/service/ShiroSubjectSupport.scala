package allawala.chassis.auth.shiro.service

import org.apache.shiro.subject.Subject

trait ShiroSubjectSupport {
  def buildSubject: Subject = (new Subject.Builder).buildSubject
}
