package models

case class User(
                 id: Long,
                 username: String,
                 password: String,
                 role: String,
                 name: String,
                 department: String,
                 email: String
               )

