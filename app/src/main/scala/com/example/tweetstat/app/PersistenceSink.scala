// package com.example.tweetstat.app

// import com.example.tweetstat.StatsDiff
// import journal.Logger

// import scalaz.{Id, Reader}
// import scalaz.concurrent.Task
// import scalaz.stream.{Channel, Process, Sink}


// object PersistenceSink {
//   val log = Logger[this.type]

//   import doobie.imports._

//   val doobieH2Storage = new Storage[Task, Int] {
//     val xa = DriverManagerTransactor[Task](
//       "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
//     )

//     def insert(name: String, age: Option[Short]): Update0 =
//       sql"insert into person (name, age) values ($name, $age)".update


//     def x: Process[Task, Unit] = insert1("foo", Some(1)).process
//   }

//   def value[R]: Reader[Storage[Task, R], Channel[Task, StatsDiff, R]] = Reader { (stg: Storage[Task,R]) =>
//     scalaz.stream.channel.lift(stg.save)
//   }

// }
