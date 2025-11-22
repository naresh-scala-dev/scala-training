package service

import jakarta.mail._
import jakarta.mail.internet._
import java.util.Properties
import com.typesafe.config.Config

class EmailService(config: Config) {


  private val mode = config.getString("email.mode")
  private val defaultFrom = config.getString("email.from")
  private val smtpHost = config.getString("email.smtp.host")
  private val smtpPort = config.getInt("email.smtp.port")
  private val smtpUser = config.getString("email.smtp.user")
  private val smtpPass = config.getString("email.smtp.pass")

  def sendEmail(to: String, subject: String, body: String, from: Option[String] = None): Unit = {
    mode match {
      case "console" =>
        println(
          s"""
             |--- EMAIL (console) ---
             |From: ${from.getOrElse(defaultFrom)}
             |To: $to
             |Subject: $subject
             |Body:
             |$body
             |-----------------------
             |""".stripMargin)

      case "smtp" =>
        val props = new Properties()
        props.put("mail.smtp.auth", "true")
        props.put("mail.smtp.starttls.enable", "true")
        props.put("mail.smtp.host", smtpHost)
        props.put("mail.smtp.port", smtpPort.toString)

        val session = Session.getInstance(props, new Authenticator() {
          override protected def getPasswordAuthentication: PasswordAuthentication =
            new PasswordAuthentication(smtpUser, smtpPass)
        })

        try {
          val msg = new MimeMessage(session)
          val sender = from.getOrElse(defaultFrom)
          msg.setFrom(new InternetAddress(sender))


          val recipients: Array[Address] = InternetAddress.parse(to).map(_.asInstanceOf[Address])
          msg.setRecipients(Message.RecipientType.TO, recipients)

          msg.setSubject(subject)
          msg.setText(body)

          Transport.send(msg)
          println(s"[EmailService] SMTP email sent → $to from $sender")

        } catch {
          case ex: Exception =>
            println(s"[EmailService] SMTP send failed: ${ex.getMessage}")
            ex.printStackTrace()
        }

      case other =>
        println(s"[EmailService] Unknown email mode=$other. Printing only.")
        println(s"From: ${from.getOrElse(defaultFrom)}\nTo: $to\nSubject: $subject\nBody:\n$body")
    }
  }
}
