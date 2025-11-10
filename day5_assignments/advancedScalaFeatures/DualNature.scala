object DualNature {

  object Email {
    def apply(user: String, domain: String): String =
      s"$user@$domain"

    def unapply(email: String): Option[(String, String)] =
      val parts = email.split("@")
      if parts.length == 2 then Some(parts(0), parts(1))
      else None
  }

  def main(args: Array[String]): Unit = {
    val e = Email("naresh", "mail.com")
    println(e)

    e match
      case Email(user, domain) =>
        println(s"User: $user, Domain: $domain")
      case _ =>
        println("Not a valid email address")
  }
}
