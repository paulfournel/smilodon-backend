# Smilodon

This is the backend of the Smilodon project. The objective is to create a decentralized social network for athletes. 
This project is using the activity pub protocol and therefore is compatible with Fediverse ecosystem.

## Features

- Fetches and processes user activities from Strava API.
- Supports processing of all users and recent activities.
- Communicates with other servers using the ActivityPub protocol.
- Compatible with Mastodon.
- Exposes a REST API for clients to interact with.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

- JDK 11
- Maven
- PostgreSQL

### Installing

1. Clone the repository
2. Navigate to the project directory
3. Run `mvn clean install` to build the project
4. Create a PostgreSQL server `docker run --name postgres -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 postgres`
5. Create a database `CREATE DATABASE smilodon;`
6. Run the project using `mvn spring-boot:run`

## Built With

- [Kotlin](https://kotlinlang.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Maven](https://maven.apache.org/)
- [PostgreSQL](https://www.postgresql.org/)

## Contributing

If you wish to contribute to this project, please contact me at smilodon.social@gmail.com

## Authors

- Paul Fournel

See also the list of [contributors](https://github.com/paulfournel/smilodon/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the `LICENSE.md` file for details

## Acknowledgments

- Strava API