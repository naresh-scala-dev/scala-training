package library.main

import library.items._
import library.users._
import library.operations.LibraryOperations._
import library.operations.LibraryOperations.given

object LibraryMain {
  def main(args: Array[String]): Unit = {
    val alice = Member("Alice")
    val book1: Book = Book("Scala Programming")
    borrow(book1)(using alice)

    val dvd1: DVD = DVD("Inception")
    borrow(dvd1) // uses given defaultMember

    borrow(
      "Harry Potter"
    ) // uses given Conversion[String, Book] and defaultMember

    val items: List[ItemType] = List(
      Book("FP in Scala"),
      Magazine("Science Today"),
      DVD("Matrix")
    )
    items.foreach(itemDescription)
  }
}
