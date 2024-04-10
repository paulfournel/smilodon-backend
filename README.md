# Smilodon

Strava Meets the Fediverse: How to Go Beyond Proof of Concepts?

I've noticed several enthusiasts dabbling with decentralized versions of Strava, sparking an idea to push these concepts further. I'm looking to ignite a conversation and connect with motivated individuals interested in launching an open-source project centered around a decentralized sports network. To kick things off, I've developed a proof of concept to showcase our potential starting point. If this piques your interest and you're eager to contribute your ideas, skills, or feedback, please drop a comment below. Let's connect and explore how we can turn this vision into reality together.

This project is using the activity pub protocol and therefore is compatible with Fediverse ecosystem.

The frontend is available [here](https://github.com/paulfournel/smilodon-frontend).

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