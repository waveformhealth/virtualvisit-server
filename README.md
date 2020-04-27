# Virtual Visit Server

## About

This is the server component of Virtual Visit, a product for making video calls simple for patients in hospitals.
This project was created during the [Twilio x DEV community hackathon](https://dev.to/devteam/announcing-the-twilio-hackathon-on-dev-2lh8).

### How it works

This application creates a web service to manage Virtual Visits (video calls) using [Twilio Programmable Video](https://www.twilio.com/docs/video).
It also uses [Twilio Programmable SMS](https://www.twilio.com/sms) for sending Virtual Visit invitations.

## Features

- Web server using [Ktor](https://ktor.io/)
- `Dockerfile` for easy development and deployment using [Docker](https://www.docker.com/)
- One click deploy button for Heroku

## Set up

> The following steps assume you are running Linux but should be adaptable for Mac or Windows

### Requirements

- A Twilio account - [sign up](https://www.twilio.com/try-twilio)
- [Docker](https://docs.docker.com/get-docker/) (optional - for deployment)

### Environment variables

Before we begin, we need to collect
all the environment variables we need to run the application:

| Environment&nbsp;Variable | Description                                                                                                                                                  |
| :------------------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| TWILIO_ACCOUNT_SID        | Your primary Twilio account identifier - find this [in the Console](https://www.twilio.com/console).                                                         |
| TWILIO_AUTH_TOKEN         | Used to authenticate - [just like the above, you'll find this here](https://www.twilio.com/console).                                                         |
| TWILIO_API_KEY            | Used to grant access tokens - find this [in the console](https://www.twilio.com/console/project/api-keys).                                                         |
| TWILIO_API_SECRET         | Also used to grant access tokens - [and also found in the console](https://www.twilio.com/console/project/api-keys).                                                         |
| TWILIO_PHONE_NUMBER       | A Twilio phone number - you can [get one here](https://www.twilio.com/console/phone-numbers/incoming) |
| INVITE_BASE_URL           | The base URL sent as part of the Virtual Visit invitation |

### Local development

The easiest way to getting started with development on the project is to use [Intellij IDEA](https://www.jetbrains.com/idea/download/).

After installation, start Intellij and create a new project from version control:

> File > New > Project from Version Control...

Ensuring "Git" is selected, enter this [repository URL](https://github.com/waveformhealth/virtualvisit-server) and then click "Clone".

With the new project opened, edit your configuration to set the required [environment variables](#environment-variables):

1. Run > Edit Configurations...
1. With the active configuration selected, click the Copy icon
1. Ensure the "Store as project file" toggle is **not** selected
1. Select the "Environment variables" "Browse" button to update the environment variables for this configuration

### Local deployment (with Docker)

1. Clone this repository and `cd` into it

```bash
git clone git@github.com:waveformhealth/virtualvisit-server.git
cd virtualvisit-server
```

2. Build the Docker image

```bash
sudo docker build -t virtualvisit-app .
```

3. Create the env-file for Docker

Copy the `env.list.dev` file to `env.list`:

```bash
cp env.list.dev env.list
```

Now edit `env.list` and add your [environment variables](#environment variables).

4. Run the application

```bash
sudo docker run -m512M --cpus 2 -it --env-file ./env.list -p 8080:8080 --rm virtualvisit-app
```

That's it! The server can now be accessed at `localhost:8080`. See the [API docs](#API) to discover the APIs.

### Cloud deployment

In addition to trying out this application locally, you can easily deploy it to a variety of host services as a Docker container.

You can also try out one click deployments below.

| Service                           |                                                                                                                                                                                                                           |
| :-------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| [Heroku](https://www.heroku.com/) | [![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)                                                                                                                                       |

## API

**NOTE**: The `secret` used below refers to a code randomly generated by the server.
If specified in the examples below, this code must be provided by the client in order to access the resource.
This is only for development and testing purposes. This mechanism should be replaced before deploying to production.

### Create a room

```plaintext
POST /v1/room
```

```shell
curl -u secret: -X POST "https://example.com/v1/room"
```

Response example:

```json
{
  "sid": "RM00000000000000000000"
}
```

### Request a token

```plaintext
GET /v1/token
```

| Attribute | Type    | Required      | Description                                |
| --------- | ------- | ------------- | ------------------------------------------ |
| `room`    | string  | yes           | The room `sid` a token is being requested for |
| `code`    | string  | no (optional) | The room access code. Required if an Authentication token is not provided |

```shell
curl -u secret: -X GET "https://example.com/v1/token?room=RM00000000000000000000"
```

> The `secret` here is optional if a valid room and room access `code` are provided

Response example:

```json
{
  "token": "0000000000000000000000"
}
```

### Send an invitation

```plaintext
POST /v1/invitation
```

| Attribute | Type    | Required      | Description                                |
| --------- | ------- | ------------- | ------------------------------------------ |
| `room`    | string  | yes           | The room `sid` to include in the invitation |
| `phone`   | string  | yes           | The phone number to send the invitation to via SMS |

```shell
curl -u secret: -X POST -H "Content-Type: application/json" -d '{"room": "RM00000000000000000000", "phone": "0000000000"}' "https://example.com/v1/invitation"
```

Response example:

```plaintext
202 Accepted
```

## License

[MIT](./LICENSE)
