package library.operations

import library.items._
import library.users._

object LibraryOperations {

  import scala.language.implicitConversions

  given defaultMember: Member = Member("Default Member")

  def borrow(item: ItemType)(using member: Member): Unit = {
    member.borrowItem(item)
  }

  def itemDescription(item: ItemType): Unit = item match {
    case Book(title)     => println(s"Book: $title")
    case Magazine(title) => println(s"Magazine: $title")
    case DVD(title)      => println(s"DVD: $title")
  }

  given Conversion[String, Book] with
    def apply(title: String): Book = Book(title)
}
