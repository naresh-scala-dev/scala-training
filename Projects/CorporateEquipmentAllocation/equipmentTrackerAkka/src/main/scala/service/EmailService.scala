package service

import jakarta.mail._
import jakarta.mail.internet._
import java.util.Properties

class EmailService(
                    smtpHost: String = "localhost",
                    smtpPort: Int = 1025,
                    defaultFrom: String = "noreply@company.com"
                  ) {

  private val props = new Properties()
  props.put("mail.smtp.host", smtpHost)
  props.put("mail.smtp.port", smtpPort.toString)
  props.put("mail.smtp.auth", "false")
  props.put("mail.smtp.starttls.enable", "false")

  private val session: Session = Session.getInstance(props)

  def sendEmail(from: Option[String], to: String, subject: String, body: String): Unit = {

    val message = new MimeMessage(session)

    val senderAddress = from.getOrElse(defaultFrom)
    message.setFrom(new InternetAddress(senderAddress))

    val addresses: Array[Address] =
      InternetAddress.parse(to).map(addr => addr.asInstanceOf[Address])

    message.setRecipients(Message.RecipientType.TO, addresses)

    message.setSubject(subject)
    message.setText(body)

    Transport.send(message)
    println(s"[EmailService] Email sent to: $to, from: $senderAddress")
  }
}
